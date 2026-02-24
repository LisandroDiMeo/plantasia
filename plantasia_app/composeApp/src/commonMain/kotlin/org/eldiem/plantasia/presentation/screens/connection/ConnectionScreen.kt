package org.eldiem.plantasia.presentation.screens.connection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.eldiem.plantasia.domain.model.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    onBack: () -> Unit,
    viewModel: ConnectionViewModel = viewModel { ConnectionViewModel() }
) {
    val connectionState by viewModel.connectionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connection") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("\u2190 Back")
                    }
                }
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Plantasia Device",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "SSID: ${ConnectionViewModel.SSID}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Status indicator
                    val (statusText, statusColor) = when (connectionState) {
                        is ConnectionState.Disconnected -> "Disconnected" to MaterialTheme.colorScheme.error
                        is ConnectionState.Connecting -> "Connecting..." to MaterialTheme.colorScheme.tertiary
                        is ConnectionState.Connected -> "Connected" to MaterialTheme.colorScheme.primary
                        is ConnectionState.Error -> "Error: ${(connectionState as ConnectionState.Error).message}" to MaterialTheme.colorScheme.error
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor
                    )

                    if (connectionState is ConnectionState.Connecting) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator()
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    when (connectionState) {
                        is ConnectionState.Connected -> {
                            OutlinedButton(
                                onClick = { viewModel.disconnect() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Disconnect")
                            }
                        }
                        is ConnectionState.Connecting -> { /* show nothing while connecting */ }
                        else -> {
                            Button(
                                onClick = { viewModel.connect() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Connect to Plantasia")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Make sure you are near the Plantasia device and WiFi is enabled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
