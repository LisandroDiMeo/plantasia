package org.eldiem.plantasia

import androidx.compose.ui.window.ComposeUIViewController
import org.eldiem.plantasia.data.wifi.WifiConnector
import org.eldiem.plantasia.di.AppDependencies

fun MainViewController() = run {
    AppDependencies.wifiConnector = WifiConnector()
    ComposeUIViewController { App() }
}
