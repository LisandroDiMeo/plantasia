package org.eldiem.plantasia.data.repository

import org.eldiem.plantasia.data.datasource.CustomPlantStorage
import org.eldiem.plantasia.data.datasource.PlantCatalogueDataSource
import org.eldiem.plantasia.domain.model.Plant
import org.eldiem.plantasia.domain.repository.PlantRepository

class PlantRepositoryImpl(
    private val dataSource: PlantCatalogueDataSource,
    private val customPlantStorage: CustomPlantStorage
) : PlantRepository {

    override suspend fun getPlants(): List<Plant> {
        val bundled = dataSource.getPlants()
        val custom = customPlantStorage.getCustomPlants()
        return bundled + custom
    }

    override suspend fun getPlant(id: String): Plant? =
        getPlants().find { it.id == id }

    override suspend fun addPlant(plant: Plant, imageBytes: ByteArray) {
        customPlantStorage.savePlant(plant, imageBytes)
    }

    override fun getCustomPlantImage(plantId: String): ByteArray? =
        customPlantStorage.getPlantImage(plantId)
}
