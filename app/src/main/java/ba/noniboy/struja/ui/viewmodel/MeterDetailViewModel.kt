package ba.noniboy.struja.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ba.noniboy.struja.data.local.entity.BillEntity
import ba.noniboy.struja.data.local.entity.MeterEntity
import ba.noniboy.struja.data.local.entity.ReadingEntity
import ba.noniboy.struja.data.repository.BillRepository
import ba.noniboy.struja.data.repository.MeterRepository
import ba.noniboy.struja.data.repository.ReadingRepository
import ba.noniboy.struja.data.repository.TariffRepository
import ba.noniboy.struja.domain.outlook.MonthKey
import ba.noniboy.struja.domain.outlook.OutlookCalculator
import ba.noniboy.struja.domain.models.TariffRates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the MeterDetail screen.
 * Provides meter info, readings, bills, and monthly outlook.
 */
@HiltViewModel
class MeterDetailViewModel @Inject constructor(
    private val meterRepository: MeterRepository,
    private val readingRepository: ReadingRepository,
    private val billRepository: BillRepository,
    private val tariffRepository: TariffRepository
) : ViewModel() {

    private val _meter = MutableStateFlow<MeterEntity?>(null)
    val meter: StateFlow<MeterEntity?> = _meter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val readings: StateFlow<List<ReadingEntity>> = _meter
        .flatMapLatest { m ->
            if (m != null) {
                readingRepository.getForMeter(m.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val bills: StateFlow<List<BillEntity>> = _meter
        .flatMapLatest { m ->
            if (m != null) {
                billRepository.getAllForMeter(m.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Load rates as a state flow (lazy init) with fallback to default rates
    private val ratesState: StateFlow<TariffRates> = flow {
        try {
            emit(tariffRepository.getRates())
        } catch (e: Exception) {
            emit(TariffRates()) // DEFAULT_RATES fallback
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TariffRates() // DEFAULT_RATES fallback
    )

    val monthOutlook: StateFlow<ba.noniboy.struja.domain.outlook.MonthOutlook?> = combine(
        readings,
        meter,
        ratesState
    ) { readingsList, m, rates ->
        if (m == null) return@combine null
        val monthKey = OutlookCalculator.currentMonthKey()
        OutlookCalculator.buildMonthOutlook(
            readings = readingsList,
            monthKey = monthKey,
            rates = rates,
            approvedKw = m.approvedKw
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun loadMeter(meterId: String) {
        viewModelScope.launch {
            _meter.value = meterRepository.get(meterId)
        }
    }

    fun deleteReading(reading: ReadingEntity) {
        viewModelScope.launch {
            readingRepository.delete(reading)
        }
    }
}
