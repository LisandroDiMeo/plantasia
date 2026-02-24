package org.eldiem.plantasia.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceStatus(
    val water: Int,
    val days: Int,
    val plantId: String = "",
    val lastWaterTimestamp: Long = 0
)
