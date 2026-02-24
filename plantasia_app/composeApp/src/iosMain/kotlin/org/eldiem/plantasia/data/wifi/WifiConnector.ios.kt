package org.eldiem.plantasia.data.wifi

import kotlinx.coroutines.suspendCancellableCoroutine
import org.eldiem.plantasia.domain.model.ConnectionState
import platform.NetworkExtension.NEHotspotConfiguration
import platform.NetworkExtension.NEHotspotConfigurationManager
import kotlin.coroutines.resume

actual class WifiConnector {
    private var isConnected = false

    actual suspend fun connect(ssid: String, password: String): ConnectionState {
        return suspendCancellableCoroutine { continuation ->
            val configuration = NEHotspotConfiguration(ssid, password, false)
            configuration.joinOnce = true

            NEHotspotConfigurationManager.sharedManager.applyConfiguration(configuration) { error ->
                if (error == null) {
                    isConnected = true
                    if (continuation.isActive) {
                        continuation.resume(ConnectionState.Connected)
                    }
                } else {
                    isConnected = false
                    if (continuation.isActive) {
                        continuation.resume(ConnectionState.Error(error.localizedDescription ?: "Connection failed"))
                    }
                }
            }
        }
    }

    actual suspend fun disconnect() {
        NEHotspotConfigurationManager.sharedManager.removeConfigurationForSSID("Plantasia")
        isConnected = false
    }

    actual suspend fun getConnectionState(): ConnectionState {
        return if (isConnected) ConnectionState.Connected else ConnectionState.Disconnected
    }
}
