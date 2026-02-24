package org.eldiem.plantasia.data.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import org.eldiem.plantasia.domain.model.ConnectionState
import kotlin.coroutines.resume

actual class WifiConnector(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var currentNetwork: Network? = null

    actual suspend fun connect(ssid: String, password: String): ConnectionState {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectApi29Plus(ssid, password)
        } else {
            connectLegacy(ssid, password)
        }
    }

    private suspend fun connectApi29Plus(ssid: String, password: String): ConnectionState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return ConnectionState.Error("Requires API 29+")
        }

        return suspendCancellableCoroutine { continuation ->
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    connectivityManager.bindProcessToNetwork(network)
                    currentNetwork = network
                    if (continuation.isActive) {
                        continuation.resume(ConnectionState.Connected)
                    }
                }

                override fun onUnavailable() {
                    if (continuation.isActive) {
                        continuation.resume(ConnectionState.Error("Network unavailable"))
                    }
                }

                override fun onLost(network: Network) {
                    if (network == currentNetwork) {
                        connectivityManager.bindProcessToNetwork(null)
                        currentNetwork = null
                    }
                }
            }

            connectivityManager.requestNetwork(request, callback)

            continuation.invokeOnCancellation {
                connectivityManager.unregisterNetworkCallback(callback)
                connectivityManager.bindProcessToNetwork(null)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun connectLegacy(ssid: String, password: String): ConnectionState {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE)
                as android.net.wifi.WifiManager

        if (!wifiManager.isWifiEnabled) {
            return ConnectionState.Error("WiFi is disabled")
        }

        val conf = android.net.wifi.WifiConfiguration().apply {
            SSID = "\"$ssid\""
            preSharedKey = "\"$password\""
        }

        val networkId = wifiManager.addNetwork(conf)
        if (networkId == -1) {
            return ConnectionState.Error("Failed to add network")
        }

        wifiManager.disconnect()
        val enabled = wifiManager.enableNetwork(networkId, true)
        wifiManager.reconnect()

        return if (enabled) ConnectionState.Connected
        else ConnectionState.Error("Failed to enable network")
    }

    actual suspend fun disconnect() {
        connectivityManager.bindProcessToNetwork(null)
        currentNetwork = null
    }

    actual suspend fun getConnectionState(): ConnectionState {
        if (currentNetwork != null) {
            val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return ConnectionState.Connected
            }
        }
        return ConnectionState.Disconnected
    }
}
