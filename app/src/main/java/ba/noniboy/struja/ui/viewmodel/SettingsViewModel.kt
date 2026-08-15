package ba.noniboy.struja.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ba.noniboy.struja.data.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apiKey: String = "",
    val hasApiKey: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showSuccess: Boolean = false
)

/**
 * ViewModel for the Settings screen.
 * Manages the user's API key storage (encrypted via Android Keystore).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadApiKey()
    }

    fun loadApiKey() {
        viewModelScope.launch {
            try {
                val key = userSettingsRepository.getDecryptedApiKey(DashboardViewModel.LOCAL_USER_ID)
                _uiState.value = _uiState.value.copy(
                    apiKey = key ?: "",
                    hasApiKey = !key.isNullOrBlank()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Greška: ${e.message}")
            }
        }
    }

    fun onApiKeyChanged(value: String) {
        _uiState.value = _uiState.value.copy(apiKey = value)
    }

    fun saveApiKey() {
        val key = _uiState.value.apiKey.trim()
        if (key.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "API ključ ne može biti prazan")
            return
        }
        if (!key.startsWith("AIza") && !key.startsWith("sk-") && !key.startsWith("sk-ant-")) {
            _uiState.value = _uiState.value.copy(
                error = "Neispravan format ključa. Očekivan: Google AI (AIza...), OpenAI (sk-...), ili Anthropic (sk-ant-...)."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                userSettingsRepository.saveApiKey(DashboardViewModel.LOCAL_USER_ID, key)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    hasApiKey = true,
                    showSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Greška pri čuvanju: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, showSuccess = false)
    }
}
