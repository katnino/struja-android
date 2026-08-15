package ba.noniboy.struja.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ba.noniboy.struja.data.local.entity.MeterEntity
import ba.noniboy.struja.data.repository.MeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Dashboard screen.
 * Provides a list of meters for the default local user.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val meterRepository: MeterRepository
) : ViewModel() {

    companion object {
        // Single-user app — use a fixed local user ID
        const val LOCAL_USER_ID = "local_user"
    }

    val meters: StateFlow<List<MeterEntity>> =
        meterRepository.getAll(LOCAL_USER_ID)
            .map { it }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val isEmpty: StateFlow<Boolean> = meters
        .map { it.isEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun createMeter() {
        viewModelScope.launch {
            meterRepository.createMeter(
                userId = LOCAL_USER_ID,
                name = "Novo brojilo",
                tariffGroup = "TG1",
                approvedKw = 0.0
            )
        }
    }
}
