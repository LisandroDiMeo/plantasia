package org.eldiem.plantasia.presentation.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.eldiem.plantasia.di.AppDependencies
import org.eldiem.plantasia.domain.model.DeviceStatus
import org.eldiem.plantasia.domain.model.Plant

data class PlantDetailUiState(
    val plant: Plant? = null,
    val deviceStatus: DeviceStatus? = null,
    val statusError: String? = null,
    val isDeviceConnected: Boolean = false,
    val isMatchingPlant: Boolean = false,
    val wateredRecently: Boolean = false
)

class PlantDetailViewModel(private val plantId: String) : ViewModel() {
    private val plantRepository = AppDependencies.plantRepository
    private val deviceRepository = AppDependencies.deviceRepository

    private val _uiState = MutableStateFlow(PlantDetailUiState())
    val uiState: StateFlow<PlantDetailUiState> = _uiState

    init {
        loadPlant()
        loadStatus()
    }

    private fun loadPlant() {
        viewModelScope.launch {
            _uiState.update { it.copy(plant = plantRepository.getPlant(plantId)) }
            updateDerivedState()
        }
    }

    private fun loadStatus() {
        viewModelScope.launch {
            try {
                val timestamp = kotlin.time.Clock.System.now().epochSeconds
                _uiState.update { it.copy(deviceStatus = deviceRepository.syncTime(timestamp), statusError = null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(statusError = "Not connected to device") }
            }
            updateDerivedState()
        }
    }

    fun water() {
        viewModelScope.launch {
            try {
                val status = deviceRepository.water()
                _uiState.update { it.copy(deviceStatus = status, statusError = null) }
            } catch (e: Exception) {
                val message = e.message ?: "Failed to water plant"
                _uiState.update {
                    it.copy(statusError = if ("429" in message) "Already watered today" else message)
                }
            }
            updateDerivedState()
        }
    }

    private fun updateDerivedState() {
        _uiState.update { s ->
            val connected = s.deviceStatus != null && s.statusError == null
            val matching = connected && s.plant != null && s.deviceStatus.plantId == s.plant.id
            val nowEpoch = kotlin.time.Clock.System.now().epochSeconds
            val watered = s.deviceStatus != null && s.deviceStatus.lastWaterTimestamp > 0
                && (nowEpoch - s.deviceStatus.lastWaterTimestamp) < 86400
            s.copy(isDeviceConnected = connected, isMatchingPlant = matching, wateredRecently = watered)
        }
    }
}
