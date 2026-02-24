package org.eldiem.plantasia.data.wifi

import org.eldiem.plantasia.domain.model.ConnectionState

expect class WifiConnector {
    suspend fun connect(ssid: String, password: String): ConnectionState
    suspend fun disconnect()
    suspend fun getConnectionState(): ConnectionState
}
