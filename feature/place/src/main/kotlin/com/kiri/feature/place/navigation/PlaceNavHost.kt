package com.kiri.feature.place.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.navArgument
import android.net.Uri
import com.kiri.feature.detail.presentation.MissionPreviewRoute
import com.kiri.feature.place.presentation.PlaceRoute

private const val GRAPH_ROUTE = "place_graph"
private const val START_ROUTE = "place"
private const val DETAIL_ROUTE = "place_graph/detail"

/** Registers this feature in the main tab back stack. */
fun NavGraphBuilder.placeNavHost(navController: NavHostController) {
    navigation(startDestination = START_ROUTE, route = GRAPH_ROUTE) {
        composable(START_ROUTE) {
            PlaceRoute(onBack = { navController.popBackStack() }, onMission = { id, kiriPickId ->
                val destination = "$DETAIL_ROUTE/${Uri.encode(id)}"
                navController.navigate(kiriPickId?.let { "$destination?kiriPickId=$it" } ?: destination) {
                    launchSingleTop = true
                }
            })
        }
        composable(
            "$DETAIL_ROUTE/{missionId}?kiriPickId={kiriPickId}",
            arguments = listOf(navArgument("kiriPickId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }),
        ) { entry ->
            MissionPreviewRoute(
                onBack = { navController.popBackStack() },
                showSaveAction = entry.arguments?.getString("kiriPickId") != null,
                onNearbyMission = { id -> navController.navigate("$DETAIL_ROUTE/$id") { launchSingleTop = true } },
            )
        }
    }
}
