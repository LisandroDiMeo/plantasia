package org.eldiem.plantasia.data.repository

import org.eldiem.plantasia.data.datasource.PlantCatalogueDataSource
import org.eldiem.plantasia.domain.model.Plant
import org.eldiem.plantasia.domain.repository.PlantRepository

class PlantRepositoryImpl(
    private val dataSource: PlantCatalogueDataSource
) : PlantRepository {

    override suspend fun getPlants(): List<Plant> = dataSource.getPlants()

    override suspend fun getPlant(id: String): Plant? =
        dataSource.getPlants().find { it.id == id }
}
