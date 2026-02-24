package org.eldiem.plantasia.domain.repository

import org.eldiem.plantasia.domain.model.DeviceStatus

interface DeviceRepository {
    suspend fun getStatus(): DeviceStatus
    suspend fun updateStatus(water: Int, days: Int): DeviceStatus
    suspend fun syncTime(timestamp: Long): DeviceStatus
    suspend fun water(): DeviceStatus
    suspend fun uploadPlant(imageBytes: ByteArray, plantId: String, onProgress: (Float) -> Unit = {})
    suspend fun deletePlant()
}
