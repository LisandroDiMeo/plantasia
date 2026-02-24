package org.eldiem.plantasia.presentation.screens.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eldiem.plantasia.di.AppDependencies
import org.eldiem.plantasia.domain.model.ConnectionState

class ConnectionViewModel : ViewModel() {
    companion object {
        const val SSID = "Plantasia"
        const val PASSWORD = "plantasia123"
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    init {
        checkConnection()
    }

    private fun checkConnection() {
        if (!AppDependencies.isWifiConnectorInitialized) return
        viewModelScope.launch {
            _connectionState.value = AppDependencies.wifiConnector.getConnectionState()
        }
    }

    fun connect() {
        if (!AppDependencies.isWifiConnectorInitialized) {
            _connectionState.value = ConnectionState.Error("WiFi not available")
            return
        }
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            _connectionState.value = AppDependencies.wifiConnector.connect(SSID, PASSWORD)
        }
    }

    fun disconnect() {
        if (!AppDependencies.isWifiConnectorInitialized) return
        viewModelScope.launch {
            AppDependencies.wifiConnector.disconnect()
            _connectionState.value = ConnectionState.Disconnected
        }
    }
}
