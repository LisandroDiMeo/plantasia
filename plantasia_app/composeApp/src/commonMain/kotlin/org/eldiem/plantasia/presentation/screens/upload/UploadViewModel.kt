package org.eldiem.plantasia.presentation.screens.upload

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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

data class UploadUiState(
    val plant: Plant? = null,
    val uploadState: UploadState = UploadState.Preparing
)

class UploadViewModel(private val plantId: String) : ViewModel() {
    private val plantRepository = AppDependencies.plantRepository
    private val deviceRepository = AppDependencies.deviceRepository

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState

    private var lastBitmap: ImageBitmap? = null

    init {
        loadPlant()
    }

    private fun loadPlant() {
        viewModelScope.launch {
            _uiState.update { it.copy(plant = plantRepository.getPlant(plantId)) }
        }
    }

    fun upload(bitmap: ImageBitmap) {
        lastBitmap = bitmap
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(uploadState = UploadState.Uploading(0f)) }
                val rgb565Data = ImageConverter.bitmapToRgb565(bitmap)
                deviceRepository.uploadPlant(rgb565Data, plantId) { progress ->
                    _uiState.update { it.copy(uploadState = UploadState.Uploading(progress)) }
                }
                _uiState.update { it.copy(uploadState = UploadState.Success) }
            } catch (e: Exception) {
                _uiState.update { it.copy(uploadState = UploadState.Error(e.message ?: "Upload failed")) }
            }
        }
    }

    fun retry() {
        lastBitmap?.let { upload(it) }
    }
}
