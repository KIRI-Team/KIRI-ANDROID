package com.kiri.feature.record.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.kiri.feature.detail.presentation.DetailScreen
import com.kiri.feature.record.presentation.RecordRoute

private const val GRAPH_ROUTE = "record_graph"
private const val START_ROUTE = "record"
private const val DETAIL_ROUTE = "record_graph/detail"

/** Registers this feature in the main tab back stack. */
fun NavGraphBuilder.recordNavHost(navController: NavHostController) {
    navigation(startDestination = START_ROUTE, route = GRAPH_ROUTE) {
        composable(START_ROUTE) {
            RecordRoute(
                onScheduleClick = { navController.navigate(DETAIL_ROUTE) },
                onBucketClick = { navController.navigate(DETAIL_ROUTE) },
            )
        }
        composable(DETAIL_ROUTE) {
            DetailScreen(title = "기록 상세", onBackClick = navController::popBackStack)
        }
    }
}
