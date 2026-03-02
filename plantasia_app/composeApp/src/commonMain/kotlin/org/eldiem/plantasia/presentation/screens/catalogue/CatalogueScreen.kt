package org.eldiem.plantasia.presentation.screens.catalogue

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.eldiem.plantasia.di.AppDependencies
import org.eldiem.plantasia.domain.model.ConnectionState
import org.eldiem.plantasia.domain.model.Plant
import org.eldiem.plantasia.presentation.components.decodeByteArrayToImageBitmap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import plantasia.composeapp.generated.resources.Res
import plantasia.composeapp.generated.resources.fern
import plantasia.composeapp.generated.resources.monstera
import plantasia.composeapp.generated.resources.rubber_plant
import plantasia.composeapp.generated.resources.succulent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueScreen(
    uiState: CatalogueUiState,
    onPlantClick: (String) -> Unit,
    onConnectionClick: () -> Unit,
    onCheckConnection: () -> Unit,
    onCreatePlant: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        onCheckConnection()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plantasia") },
                actions = {
                    val (pillColor, pillText) = when (uiState.connectionState) {
                        is ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer to "Connected"
                        is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiaryContainer to "Connecting..."
                        else -> MaterialTheme.colorScheme.surfaceVariant to "Disconnected"
                    }
                    val pillTextColor = when (uiState.connectionState) {
                        is ConnectionState.Connected -> MaterialTheme.colorScheme.onPrimaryContainer
                        is ConnectionState.Connecting -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        onClick = onConnectionClick,
                        modifier = Modifier.padding(end = 12.dp),
                        color = pillColor,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = pillText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = pillTextColor
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreatePlant) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.plants) { plant ->
                    PlantCard(
                        plant = plant,
                        isOnDevice = uiState.devicePlantId == plant.id,
                        onClick = { onPlantClick(plant.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlantCard(plant: Plant, isOnDevice: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            val drawableRes = plantDrawableMap[plant.imageRes]
            val customBitmap = if (drawableRes == null && plant.imageRes.startsWith("custom_")) {
                remember(plant.id) {
                    AppDependencies.plantRepository.getCustomPlantImage(plant.id)?.let {
                        decodeByteArrayToImageBitmap(it)
                    }
                }
            } else null

            if (drawableRes != null) {
                PlantImageBox(isOnDevice = isOnDevice) {
                    Image(
                        painter = painterResource(drawableRes),
                        contentDescription = plant.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else if (customBitmap != null) {
                PlantImageBox(isOnDevice = isOnDevice) {
                    Image(
                        bitmap = customBitmap,
                        contentDescription = plant.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\uD83C\uDF31")
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plant.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlantImageBox(isOnDevice: Boolean, content: @Composable () -> Unit) {
    Box {
        content()
        if (isOnDevice) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "On device",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

val plantDrawableMap: Map<String, DrawableResource> = mapOf(
    "monstera" to Res.drawable.monstera,
    "succulent" to Res.drawable.succulent,
    "fern" to Res.drawable.fern,
    "rubber_plant" to Res.drawable.rubber_plant,
)
