package org.eldiem.plantasia.di

import org.eldiem.plantasia.data.datasource.DeviceApi
import org.eldiem.plantasia.data.datasource.PlantCatalogueDataSource
import org.eldiem.plantasia.data.repository.DeviceRepositoryImpl
import org.eldiem.plantasia.data.repository.PlantRepositoryImpl
import org.eldiem.plantasia.data.wifi.WifiConnector
import org.eldiem.plantasia.domain.repository.DeviceRepository
import org.eldiem.plantasia.domain.repository.PlantRepository

object AppDependencies {
    private val deviceApi by lazy { DeviceApi() }
    private val plantCatalogueDataSource by lazy { PlantCatalogueDataSource() }

    val plantRepository: PlantRepository by lazy { PlantRepositoryImpl(plantCatalogueDataSource) }
    val deviceRepository: DeviceRepository by lazy { DeviceRepositoryImpl(deviceApi) }

    lateinit var wifiConnector: WifiConnector

    val isWifiConnectorInitialized: Boolean
        get() = ::wifiConnector.isInitialized
}
