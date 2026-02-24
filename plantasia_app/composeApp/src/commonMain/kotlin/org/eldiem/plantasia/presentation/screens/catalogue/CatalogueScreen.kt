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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.eldiem.plantasia.domain.model.ConnectionState
import org.eldiem.plantasia.domain.model.Plant
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
    onCheckConnection: () -> Unit
) {
    LaunchedEffect(Unit) {
        println("checking connection")
        onCheckConnection()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plantasia") },
                actions = {
                    IconButton(onClick = onConnectionClick) {
                        val tint = when (uiState.connectionState) {
                            is ConnectionState.Connected -> MaterialTheme.colorScheme.primary
                            is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text("\u26A1", color = tint)
                    }
                }
            )
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
            if (drawableRes != null) {
                Box {
                    Image(
                        painter = painterResource(drawableRes),
                        contentDescription = plant.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
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

val plantDrawableMap: Map<String, DrawableResource> = mapOf(
    "monstera" to Res.drawable.monstera,
    "succulent" to Res.drawable.succulent,
    "fern" to Res.drawable.fern,
    "rubber_plant" to Res.drawable.rubber_plant,
)
