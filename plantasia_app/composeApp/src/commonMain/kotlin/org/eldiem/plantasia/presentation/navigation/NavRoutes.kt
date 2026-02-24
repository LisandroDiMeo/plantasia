package org.eldiem.plantasia.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object CatalogueRoute

@Serializable
data class PlantDetailRoute(val plantId: String)

@Serializable
object ConnectionRoute

@Serializable
data class UploadRoute(val plantId: String)
