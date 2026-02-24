package org.eldiem.plantasia.data.datasource

import kotlinx.serialization.json.Json
import org.eldiem.plantasia.domain.model.Plant
import org.jetbrains.compose.resources.ExperimentalResourceApi
import plantasia.composeapp.generated.resources.Res

class PlantCatalogueDataSource {
    private var cachedPlants: List<Plant>? = null

    @OptIn(ExperimentalResourceApi::class)
    suspend fun getPlants(): List<Plant> {
        cachedPlants?.let { return it }
        val jsonString = Res.readBytes("files/plant_catalogue.json").decodeToString()
        val plants = Json.decodeFromString<List<Plant>>(jsonString)
        cachedPlants = plants
        return plants
    }
}
