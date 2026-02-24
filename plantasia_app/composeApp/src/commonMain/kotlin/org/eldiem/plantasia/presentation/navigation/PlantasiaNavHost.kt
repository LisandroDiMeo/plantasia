package org.eldiem.plantasia.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.eldiem.plantasia.presentation.screens.catalogue.CatalogueScreen
import org.eldiem.plantasia.presentation.screens.connection.ConnectionScreen
import org.eldiem.plantasia.presentation.screens.detail.PlantDetailScreen
import org.eldiem.plantasia.presentation.screens.upload.UploadScreen

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
            CatalogueScreen(
                onPlantClick = { plantId ->
                    navController.navigate(PlantDetailRoute(plantId))
                },
                onConnectionClick = {
                    navController.navigate(ConnectionRoute)
                }
            )
        }
        composable<PlantDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PlantDetailRoute>()
            PlantDetailScreen(
                plantId = route.plantId,
                onSendClick = { plantId ->
                    navController.navigate(UploadRoute(plantId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable<ConnectionRoute> {
            ConnectionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<UploadRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<UploadRoute>()
            UploadScreen(
                plantId = route.plantId,
                onDone = {
                    navController.popBackStack(CatalogueRoute, inclusive = false)
                }
            )
        }
    }
}
