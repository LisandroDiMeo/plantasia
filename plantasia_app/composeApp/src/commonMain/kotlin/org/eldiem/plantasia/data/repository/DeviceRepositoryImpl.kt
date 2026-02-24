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

    override suspend fun uploadPlant(imageBytes: ByteArray, onProgress: (Float) -> Unit) =
        api.uploadPlant(imageBytes, onProgress)

    override suspend fun deletePlant() = api.deletePlant()
}
