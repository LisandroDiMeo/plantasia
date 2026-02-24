package org.eldiem.plantasia.presentation.screens.upload

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eldiem.plantasia.data.ImageConverter
import org.eldiem.plantasia.di.AppDependencies
import org.eldiem.plantasia.domain.model.Plant

sealed class UploadState {
    data object Preparing : UploadState()
    data class Uploading(val progress: Float) : UploadState()
    data object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

class UploadViewModel(private val plantId: String) : ViewModel() {
    private val plantRepository = AppDependencies.plantRepository
    private val deviceRepository = AppDependencies.deviceRepository

    private val _plant = MutableStateFlow<Plant?>(null)
    val plant: StateFlow<Plant?> = _plant

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Preparing)
    val uploadState: StateFlow<UploadState> = _uploadState

    init {
        loadPlant()
    }

    private fun loadPlant() {
        viewModelScope.launch {
            _plant.value = plantRepository.getPlant(plantId)
        }
    }

    fun upload(bitmap: ImageBitmap) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Uploading(0f)
                val rgb565Data = ImageConverter.bitmapToRgb565(bitmap)
                deviceRepository.uploadPlant(rgb565Data) { progress ->
                    _uploadState.value = UploadState.Uploading(progress)
                }
                _uploadState.value = UploadState.Success
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Upload failed")
            }
        }
    }
}
