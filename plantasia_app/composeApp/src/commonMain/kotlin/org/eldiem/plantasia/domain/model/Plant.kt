package org.eldiem.plantasia.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Plant(
    val id: String,
    val name: String,
    val description: String,
    val imageRes: String
)
