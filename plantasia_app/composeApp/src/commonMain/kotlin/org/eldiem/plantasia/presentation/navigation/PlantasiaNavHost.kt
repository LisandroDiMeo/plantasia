package org.eldiem.plantasia.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.eldiem.plantasia.presentation.screens.catalogue.CatalogueScreen
import org.eldiem.plantasia.presentation.screens.catalogue.CatalogueViewModel
import org.eldiem.plantasia.presentation.screens.connection.ConnectionScreen
import org.eldiem.plantasia.presentation.screens.detail.PlantDetailScreen
import org.eldiem.plantasia.presentation.screens.detail.PlantDetailViewModel
import org.eldiem.plantasia.presentation.screens.upload.UploadScreen
import org.eldiem.plantasia.presentation.screens.upload.UploadViewModel

@Composable
fun PlantasiaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = CatalogueRoute,
        modifier = modifier
    ) {
        composable<CatalogueRoute> {
            val vm = viewModel { CatalogueViewModel() }
            val uiState by vm.uiState.collectAsState()
            CatalogueScreen(
                uiState = uiState,
                onPlantClick = { plantId ->
                    navController.navigate(PlantDetailRoute(plantId))
                },
                onConnectionClick = {
                    navController.navigate(ConnectionRoute)
                },
                onCheckConnection = vm::checkConnection
            )
        }
        composable<PlantDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PlantDetailRoute>()
            val vm = viewModel { PlantDetailViewModel(route.plantId) }
            val uiState by vm.uiState.collectAsState()
            val lifecycleOwner = LocalLifecycleOwner.current
            val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
            val isInteractive = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
            PlantDetailScreen(
                uiState = uiState,
                onSendClick = { plantId ->
                    navController.navigate(UploadRoute(plantId))
                },
                onWater = vm::water,
                onBack = { navController.popBackStack() },
                isInteractive = isInteractive
            )
        }
        composable<ConnectionRoute> {
            ConnectionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<UploadRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<UploadRoute>()
            val vm = viewModel { UploadViewModel(route.plantId) }
            val uiState by vm.uiState.collectAsState()
            val lifecycleOwner = LocalLifecycleOwner.current
            val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
            val isInteractive = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
            UploadScreen(
                uiState = uiState,
                onUpload = vm::upload,
                onRetry = vm::retry,
                onDone = {
                    navController.popBackStack(CatalogueRoute, inclusive = false)
                },
                isInteractive = isInteractive
            )
        }
    }
}
