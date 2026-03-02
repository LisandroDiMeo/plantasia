package org.eldiem.plantasia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.eldiem.plantasia.data.FileStorage
import org.eldiem.plantasia.data.wifi.WifiConnector
import org.eldiem.plantasia.di.AppDependencies

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AppDependencies.fileStorage = FileStorage(applicationContext)
        AppDependencies.wifiConnector = WifiConnector(applicationContext)

        setContent {
            App()
        }
    }
}
