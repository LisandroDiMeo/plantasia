package org.eldiem.plantasia.presentation.screens.upload

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.eldiem.plantasia.presentation.screens.catalogue.plantDrawableMap
import org.jetbrains.compose.resources.imageResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    plantId: String,
    onDone: () -> Unit,
    viewModel: UploadViewModel = viewModel { UploadViewModel(plantId) }
) {
    val plant by viewModel.plant.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()

    // Load the bitmap once and trigger upload
    val drawableRes = plant?.imageRes?.let { plantDrawableMap[it] }
    var uploadStarted by remember { mutableStateOf(false) }

    if (drawableRes != null && !uploadStarted) {
        val bitmap: ImageBitmap = imageResource(drawableRes)
        LaunchedEffect(bitmap) {
            uploadStarted = true
            viewModel.upload(bitmap)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Uploading") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            plant?.let {
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (drawableRes != null) {
                    Image(
                        bitmap = imageResource(drawableRes),
                        contentDescription = it.name,
                        modifier = Modifier.size(120.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            when (val state = uploadState) {
                is UploadState.Preparing -> {
                    Text("Preparing image...")
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
                is UploadState.Uploading -> {
                    Text("Sending to device...")
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${(state.progress * 100).toInt()}%")
                }
                is UploadState.Success -> {
                    Text(
                        text = "Upload complete!",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDone,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back to Catalogue")
                    }
                }
                is UploadState.Error -> {
                    Text(
                        text = "Upload failed",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.retry() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Retry")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onDone,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back to Catalogue")
                    }
                }
            }
        }
    }
}
