package org.eldiem.plantasia.domain.repository

import org.eldiem.plantasia.domain.model.Plant

interface PlantRepository {
    suspend fun getPlants(): List<Plant>
    suspend fun getPlant(id: String): Plant?
}
