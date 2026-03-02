package org.eldiem.plantasia.data.datasource

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.eldiem.plantasia.domain.model.DeviceStatus

class DeviceApi {
    companion object {
        private const val BASE_URL = "http://192.168.4.1"
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getStatus(): DeviceStatus {
        return client.get("$BASE_URL/status").body()
    }

    suspend fun updateStatus(water: Int, days: Int): DeviceStatus {
        return client.get("$BASE_URL/update") {
            parameter("water", water)
            parameter("days", days)
        }.body()
    }

    suspend fun uploadPlant(imageBytes: ByteArray, plantId: String, onProgress: (Float) -> Unit) {
        client.submitFormWithBinaryData(
            url = "$BASE_URL/plant",
            formData = formData {
                append("image", imageBytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"plant.bin\"")
                    append(HttpHeaders.ContentType, "application/octet-stream")
                })
            }
        ) {
            url {
                parameters.append("plantId", plantId)
            }
            onUpload { bytesSentTotal, contentLength ->
                if (contentLength != null && contentLength > 0) {
                    onProgress(bytesSentTotal.toFloat() / contentLength.toFloat())
                }
            }
        }
    }

    suspend fun syncTime(timestamp: Long): DeviceStatus {
        return client.get("$BASE_URL/sync") {
            parameter("timestamp", timestamp)
        }.body()
    }

    suspend fun water(): DeviceStatus {
        return client.post("$BASE_URL/water").body()
    }

    suspend fun deletePlant() {
        client.delete("$BASE_URL/plant")
    }
}
