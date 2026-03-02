package org.eldiem.plantasia.presentation.screens.create

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.eldiem.plantasia.di.AppDependencies
import org.eldiem.plantasia.domain.model.Plant

data class CreatePlantUiState(
    val name: String = "",
    val description: String = "",
    val imageBytes: ByteArray? = null,
    val imageBitmap: ImageBitmap? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val showImagePicker: Boolean = false
)

class CreatePlantViewModel : ViewModel() {
    private val plantRepository = AppDependencies.plantRepository

    private val _uiState = MutableStateFlow(CreatePlantUiState())
    val uiState: StateFlow<CreatePlantUiState> = _uiState

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description, error = null) }
    }

    fun onImageSelected(imageBytes: ByteArray, bitmap: ImageBitmap) {
        _uiState.update { it.copy(imageBytes = imageBytes, imageBitmap = bitmap, error = null) }
    }

    fun showImagePicker() {
        _uiState.update { it.copy(showImagePicker = true) }
    }

    fun dismissImagePicker() {
        _uiState.update { it.copy(showImagePicker = false) }
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Plant name is required") }
            return
        }
        if (state.imageBytes == null) {
            _uiState.update { it.copy(error = "An image is required") }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val timestamp = kotlin.time.Clock.System.now().epochSeconds
                val id = "custom_$timestamp"
                val plant = Plant(
                    id = id,
                    name = state.name.trim(),
                    description = state.description.trim(),
                    imageRes = id
                )
                plantRepository.addPlant(plant, state.imageBytes)
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "Failed to save plant") }
            }
        }
    }
}
