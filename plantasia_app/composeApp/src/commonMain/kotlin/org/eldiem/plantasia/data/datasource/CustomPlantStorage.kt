package org.eldiem.plantasia.data.datasource

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eldiem.plantasia.data.FileStorage
import org.eldiem.plantasia.domain.model.Plant

class CustomPlantStorage(private val fileStorage: FileStorage) {
    companion object {
        private const val PLANTS_FILE = "custom_plants.json"
        private fun imageFile(plantId: String) = "custom_plant_${plantId}.png"
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun getCustomPlants(): List<Plant> {
        val text = fileStorage.readText(PLANTS_FILE) ?: return emptyList()
        return try {
            json.decodeFromString<List<Plant>>(text)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePlant(plant: Plant, imageBytes: ByteArray) {
        fileStorage.writeBytes(imageFile(plant.id), imageBytes)
        val plants = getCustomPlants().toMutableList()
        plants.removeAll { it.id == plant.id }
        plants.add(plant)
        fileStorage.writeText(PLANTS_FILE, json.encodeToString(plants))
    }

    fun getPlantImage(plantId: String): ByteArray? {
        return fileStorage.readBytes(imageFile(plantId))
    }
}
