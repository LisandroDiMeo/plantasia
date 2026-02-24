package org.eldiem.plantasia.presentation.screens.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.eldiem.plantasia.presentation.screens.catalogue.plantDrawableMap
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    uiState: PlantDetailUiState,
    onSendClick: (String) -> Unit,
    onWater: () -> Unit,
    onBack: () -> Unit,
    isInteractive: Boolean = true
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.plant?.name ?: "Plant Details") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("\u2190 Back")
                    }
                }
            )
        }
    ) { padding ->
        val currentPlant = uiState.plant
        if (currentPlant == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val drawableRes = plantDrawableMap[currentPlant.imageRes]
                if (drawableRes != null) {
                    Image(
                        painter = painterResource(drawableRes),
                        contentDescription = currentPlant.name,
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentPlant.name,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentPlant.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error card — show when there's an error and plant doesn't match device
                if (uiState.statusError != null && !uiState.isMatchingPlant) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = uiState.statusError,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Device status card — only show when this plant matches the device
                if (uiState.isMatchingPlant) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Device Status",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "\uD83D\uDCA7",
                                        style = MaterialTheme.typography.headlineLarge
                                    )
                                    Text("Water: ${uiState.deviceStatus!!.water}")
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "\uD83D\uDCC5",
                                        style = MaterialTheme.typography.headlineLarge
                                    )
                                    Text("Days: ${uiState.deviceStatus!!.days}")
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (uiState.wateredRecently) {
                                Button(
                                    onClick = {},
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = false
                                ) {
                                    Text("Watered today")
                                }
                            } else {
                                Button(
                                    onClick = onWater,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = isInteractive
                                ) {
                                    Text("\uD83D\uDCA7 Water Plant")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onSendClick(currentPlant.id) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = uiState.isDeviceConnected && isInteractive
                ) {
                    Text("Send to Device")
                }
            }
        }
    }
}
