package com.kiri.feature.place.navigation

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.kiri.feature.detail.presentation.DetailScreen
import com.kiri.feature.place.presentation.PlaceRoute

private const val GRAPH_ROUTE = "place_graph"
private const val START_ROUTE = "place"
private const val DETAIL_ROUTE = "place_graph/detail"

/** Registers this feature in the main tab back stack. */
fun NavGraphBuilder.placeNavHost(navController: NavHostController) {
    navigation(startDestination = START_ROUTE, route = GRAPH_ROUTE) {
        composable(START_ROUTE) {
            PlaceRoute(onBack = { navController.popBackStack() })
        }
        composable(DETAIL_ROUTE) {
            DetailScreen(title = "장소 상세", onBackClick = navController::popBackStack)
        }
    }
}
