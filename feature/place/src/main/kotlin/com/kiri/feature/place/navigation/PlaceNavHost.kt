package com.kiri.feature.place.navigation

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
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
            PlaceRoute(onBack = { navController.popBackStack() }, onMission = { id ->
                navController.navigate("$DETAIL_ROUTE/${Uri.encode(id)}") { launchSingleTop = true }
            })
        }
        composable("$DETAIL_ROUTE/{missionId}") {
            MissionPreviewRoute(onBack = { navController.popBackStack() })
        }
    }
}
