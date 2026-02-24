package org.eldiem.plantasia.data.repository

import org.eldiem.plantasia.data.datasource.DeviceApi
import org.eldiem.plantasia.domain.model.DeviceStatus
import org.eldiem.plantasia.domain.repository.DeviceRepository

class DeviceRepositoryImpl(
    private val api: DeviceApi
) : DeviceRepository {

    override suspend fun getStatus(): DeviceStatus = api.getStatus()

    override suspend fun updateStatus(water: Int, days: Int): DeviceStatus =
        api.updateStatus(water, days)

    override suspend fun syncTime(timestamp: Long): DeviceStatus =
        api.syncTime(timestamp)

    override suspend fun water(): DeviceStatus =
        api.water()

    override suspend fun uploadPlant(imageBytes: ByteArray, plantId: String, onProgress: (Float) -> Unit) =
        api.uploadPlant(imageBytes, plantId, onProgress)

    override suspend fun deletePlant() = api.deletePlant()
}
