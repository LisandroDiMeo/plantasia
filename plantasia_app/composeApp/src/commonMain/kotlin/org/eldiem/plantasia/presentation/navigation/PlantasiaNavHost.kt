package org.eldiem.plantasia.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.eldiem.plantasia.presentation.components.ImagePicker
import org.eldiem.plantasia.presentation.components.decodeByteArrayToImageBitmap
import org.eldiem.plantasia.presentation.screens.catalogue.CatalogueScreen
import org.eldiem.plantasia.presentation.screens.catalogue.CatalogueViewModel
import org.eldiem.plantasia.presentation.screens.connection.ConnectionScreen
import org.eldiem.plantasia.presentation.screens.create.CreatePlantScreen
import org.eldiem.plantasia.presentation.screens.create.CreatePlantViewModel
import org.eldiem.plantasia.presentation.screens.create.PixelDrawScreen
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
                onCheckConnection = vm::checkConnection,
                onCreatePlant = {
                    navController.navigate(CreatePlantRoute)
                }
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
        composable<CreatePlantRoute> { backStackEntry ->
            val vm = viewModel { CreatePlantViewModel() }
            val uiState by vm.uiState.collectAsState()

            // Handle drawn image coming back from PixelDrawScreen
            val savedStateHandle = backStackEntry.savedStateHandle
            LaunchedEffect(Unit) {
                savedStateHandle.getStateFlow<ByteArray?>("drawnImageBytes", null)
                    .collect { bytes ->
                        if (bytes != null) {
                            val bitmap = decodeByteArrayToImageBitmap(bytes)
                            if (bitmap != null) {
                                vm.onImageSelected(bytes, bitmap)
                            }
                            savedStateHandle.remove<ByteArray>("drawnImageBytes")
                        }
                    }
            }

            ImagePicker(
                show = uiState.showImagePicker,
                onImagePicked = { bytes ->
                    vm.dismissImagePicker()
                    val bitmap = decodeByteArrayToImageBitmap(bytes)
                    if (bitmap != null) {
                        vm.onImageSelected(bytes, bitmap)
                    }
                },
                onDismiss = { vm.dismissImagePicker() }
            )

            CreatePlantScreen(
                uiState = uiState,
                onNameChange = vm::onNameChange,
                onDescriptionChange = vm::onDescriptionChange,
                onUploadImage = { vm.showImagePicker() },
                onDrawImage = {
                    navController.navigate(PixelDrawRoute)
                },
                onSave = vm::save,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack(CatalogueRoute, inclusive = false)
                }
            )
        }
        composable<PixelDrawRoute> {
            PixelDrawScreen(
                onDone = { pngBytes ->
                    // Pass the drawn image back to CreatePlantScreen via savedStateHandle
                    navController.previousBackStackEntry?.savedStateHandle?.set("drawnImageBytes", pngBytes)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
