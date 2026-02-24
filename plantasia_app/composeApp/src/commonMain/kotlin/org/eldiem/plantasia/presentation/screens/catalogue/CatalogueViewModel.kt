package org.eldiem.plantasia.presentation.screens.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.eldiem.plantasia.di.AppDependencies
import org.eldiem.plantasia.domain.model.ConnectionState
import org.eldiem.plantasia.domain.model.Plant

data class CatalogueUiState(
    val plants: List<Plant> = emptyList(),
    val isLoading: Boolean = true,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val devicePlantId: String? = null
)

class CatalogueViewModel : ViewModel() {
    private val plantRepository = AppDependencies.plantRepository
    private val deviceRepository = AppDependencies.deviceRepository

    private val _uiState = MutableStateFlow(CatalogueUiState())
    val uiState: StateFlow<CatalogueUiState> = _uiState

    init {
        loadPlants()
    }

    private fun loadPlants() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val plants = plantRepository.getPlants()
            _uiState.update { it.copy(plants = plants, isLoading = false) }
        }
    }

    fun checkConnection() {
        if (!AppDependencies.isWifiConnectorInitialized) return
        viewModelScope.launch {
            val state = AppDependencies.wifiConnector.getConnectionState()
            _uiState.update { it.copy(connectionState = state) }
            if (state is ConnectionState.Connected) {
                try {
                    val status = deviceRepository.getStatus()
                    _uiState.update { it.copy(devicePlantId = status.plantId) }
                } catch (_: Exception) { /* leave null */ }
            }
        }
    }
}
