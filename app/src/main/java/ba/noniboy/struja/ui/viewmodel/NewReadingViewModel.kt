package ba.noniboy.struja.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ba.noniboy.struja.data.local.entity.MeterEntity
import ba.noniboy.struja.data.local.entity.ReadingEntity
import ba.noniboy.struja.data.repository.BillRepository
import ba.noniboy.struja.data.repository.MeterRepository
import ba.noniboy.struja.data.repository.ReadingRepository
import ba.noniboy.struja.data.repository.TariffRepository
import ba.noniboy.struja.domain.models.BillResult
import ba.noniboy.struja.domain.models.TariffRates
import ba.noniboy.struja.domain.tariff.TariffCalculator
import ba.noniboy.struja.data.vision.ExtractResult
import ba.noniboy.struja.data.vision.MeterOcrProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class NewReadingUiState(
    val vt: String = "",
    val mt: String = "",
    val recordedAt: String = LocalDate.now().toString(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val previewBill: BillResult? = null,
    val ocrConfidence: String? = null,
    val ocrNote: String? = null
)

/**
 * ViewModel for the NewReading screen.
 * Handles manual/QR entry and live bill preview.
 */
@HiltViewModel
class NewReadingViewModel @Inject constructor(
    private val meterRepository: MeterRepository,
    private val readingRepository: ReadingRepository,
    private val billRepository: BillRepository,
    private val tariffRepository: TariffRepository,
    private val ocrProcessor: MeterOcrProcessor
) : ViewModel() {

    private val _meter = MutableStateFlow<MeterEntity?>(null)
    val meter: StateFlow<MeterEntity?> = _meter.asStateFlow()

    private val _rates = MutableStateFlow<TariffRates?>(null)

    private val _uiState = MutableStateFlow(NewReadingUiState())
    val uiState: StateFlow<NewReadingUiState> = _uiState.asStateFlow()

    // OCR result (for display in ExtractedPreview)
    private val _ocrResult = MutableStateFlow<ba.noniboy.struja.data.vision.ExtractResult?>(null)
    val ocrResult: StateFlow<ba.noniboy.struja.data.vision.ExtractResult?> = _ocrResult.asStateFlow()

    fun loadMeter(meterId: String) {
        viewModelScope.launch {
            _meter.value = meterRepository.get(meterId)
            _rates.value = tariffRepository.getRates()
        }
    }

    fun onVtChanged(value: String) {
        _uiState.value = _uiState.value.copy(vt = value)
        updatePreview()
    }

    fun onMtChanged(value: String) {
        _uiState.value = _uiState.value.copy(mt = value)
        updatePreview()
    }

    fun onRecordedAtChanged(value: String) {
        _uiState.value = _uiState.value.copy(recordedAt = value)
    }

    fun onOcrResult(result: ExtractResult) {
        val vtStr = result.vt?.toString() ?: ""
        val mtStr = result.mt?.toString() ?: ""
        val readingStr = result.reading?.toString()

        var newState = _uiState.value.copy(
            vt = if (vtStr.isNotEmpty()) vtStr else _uiState.value.vt,
            mt = if (mtStr.isNotEmpty()) mtStr else _uiState.value.mt,
            ocrConfidence = result.confidence.name,
            ocrNote = result.note
        )

        // For single-tariff meters, set the reading
        if (readingStr != null && readingStr.isNotEmpty()) {
            newState = newState.copy(vt = readingStr) // Will be handled differently for TG1
        }

        _uiState.value = newState
        updatePreview()
    }

    private fun updatePreview() {
        val m = _meter.value ?: return
        val rates = _rates.value ?: return
        val vt = _uiState.value.vt.toDoubleOrNull() ?: 0.0
        val mt = _uiState.value.mt.toDoubleOrNull() ?: 0.0

        val preview = TariffCalculator.calculateBill(
            vtKwh = vt,
            mtKwh = mt,
            approvedKw = m.approvedKw,
            rates = rates
        )
        _uiState.value = _uiState.value.copy(previewBill = preview)
    }

    fun saveReading(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val m = _meter.value ?: return@launch
            val vt = _uiState.value.vt.toDoubleOrNull() ?: return@launch
            val mt = _uiState.value.mt.toDoubleOrNull() ?: 0.0

            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            try {
                val rates = tariffRepository.getRates()
                val prevReading = readingRepository.getLatest(m.id)

                val reading = readingRepository.createReading(
                    meterId = m.id,
                    userId = DashboardViewModel.LOCAL_USER_ID,
                    recordedAt = _uiState.value.recordedAt,
                    vt = if (m.tariffGroup == "TG2") vt else null,
                    mt = if (m.tariffGroup == "TG2") mt else null,
                    reading = if (m.tariffGroup == "TG1") vt else null,
                    source = "manual"
                )

                // Generate bill if there's a previous reading
                prevReading?.let { prev ->
                    billRepository.generateBill(
                        meterId = m.id,
                        userId = DashboardViewModel.LOCAL_USER_ID,
                        prevReading = prev,
                        currReading = reading,
                        rates = rates,
                        approvedKw = m.approvedKw
                    )
                }

                _uiState.value = _uiState.value.copy(isSaving = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Greška: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Run OCR on the given bitmap and update the UI state with the result.
     * The result is delivered asynchronously via the ocrResult StateFlow.
     */
    fun runOcr(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            try {
                val result = ocrProcessor.extractMeterReading(bitmap)
                _ocrResult.value = result

                // Update UI state with OCR confidence
                _uiState.value = _uiState.value.copy(
                    ocrConfidence = result.confidence.name,
                    ocrNote = result.note
                )

                // For single-tariff meters, set the reading
                result.reading?.toString()?.let { readingStr ->
                    _uiState.value = _uiState.value.copy(vt = readingStr)
                }

                result.vt?.toString()?.let { vtStr ->
                    _uiState.value = _uiState.value.copy(vt = vtStr)
                }

                result.mt?.toString()?.let { mtStr ->
                    _uiState.value = _uiState.value.copy(mt = mtStr)
                }

                updatePreview()
            } catch (e: Exception) {
                Log.e("NewReadingViewModel", "OCR failed", e)
                _uiState.value = _uiState.value.copy(
                    error = "OCR greška: ${e.message}"
                )
            }
        }
    }
}