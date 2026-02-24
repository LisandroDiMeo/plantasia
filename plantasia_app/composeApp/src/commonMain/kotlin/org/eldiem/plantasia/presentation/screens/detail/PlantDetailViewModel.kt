package org.eldiem.plantasia.presentation.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eldiem.plantasia.di.AppDependencies
import org.eldiem.plantasia.domain.model.DeviceStatus
import org.eldiem.plantasia.domain.model.Plant

class PlantDetailViewModel(private val plantId: String) : ViewModel() {
    private val plantRepository = AppDependencies.plantRepository
    private val deviceRepository = AppDependencies.deviceRepository

    private val _plant = MutableStateFlow<Plant?>(null)
    val plant: StateFlow<Plant?> = _plant

    private val _deviceStatus = MutableStateFlow<DeviceStatus?>(null)
    val deviceStatus: StateFlow<DeviceStatus?> = _deviceStatus

    private val _statusError = MutableStateFlow<String?>(null)
    val statusError: StateFlow<String?> = _statusError

    init {
        loadPlant()
        loadStatus()
    }

    private fun loadPlant() {
        viewModelScope.launch {
            _plant.value = plantRepository.getPlant(plantId)
        }
    }

    private fun loadStatus() {
        viewModelScope.launch {
            try {
                val timestamp = kotlin.time.Clock.System.now().epochSeconds
                _deviceStatus.value = deviceRepository.syncTime(timestamp)
                _statusError.value = null
            } catch (_: Exception) {
                _statusError.value = "Not connected to device"
            }
        }
    }

    fun water() {
        viewModelScope.launch {
            try {
                _deviceStatus.value = deviceRepository.water()
                _statusError.value = null
            } catch (e: Exception) {
                val message = e.message ?: "Failed to water plant"
                _statusError.value = if ("429" in message) "Already watered today" else message
            }
        }
    }

    fun updateStatus(water: Int, days: Int) {
        viewModelScope.launch {
            try {
                _deviceStatus.value = deviceRepository.updateStatus(water, days)
                _statusError.value = null
            } catch (_: Exception) {
                _statusError.value = "Failed to update status"
            }
        }
    }
}
