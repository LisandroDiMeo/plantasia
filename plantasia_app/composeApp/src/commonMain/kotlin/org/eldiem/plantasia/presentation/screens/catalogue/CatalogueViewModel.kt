package org.eldiem.plantasia.presentation.screens.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eldiem.plantasia.di.AppDependencies
import org.eldiem.plantasia.domain.model.ConnectionState
import org.eldiem.plantasia.domain.model.Plant

class CatalogueViewModel : ViewModel() {
    private val plantRepository = AppDependencies.plantRepository

    private val _plants = MutableStateFlow<List<Plant>>(emptyList())
    val plants: StateFlow<List<Plant>> = _plants

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    init {
        loadPlants()
    }

    private fun loadPlants() {
        viewModelScope.launch {
            _isLoading.value = true
            _plants.value = plantRepository.getPlants()
            _isLoading.value = false
        }
    }

    fun checkConnection() {
        if (!AppDependencies.isWifiConnectorInitialized) return
        viewModelScope.launch {
            _connectionState.value = AppDependencies.wifiConnector.getConnectionState()
        }
    }
}
