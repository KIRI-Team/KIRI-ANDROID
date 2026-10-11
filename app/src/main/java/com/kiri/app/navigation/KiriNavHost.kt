package com.kiri.app.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.kiri.feature.home.navigation.homeNavHost
import com.kiri.feature.record.navigation.recordNavHost
import com.kiri.feature.place.presentation.PlaceScreen
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.kiri.feature.detail.presentation.DetailScreen
import com.kiri.feature.home.presentation.HomeScreen
import com.kiri.feature.home.presentation.InviteFamilyScreen
import com.kiri.feature.mypage.navigation.mypageGraph
import com.kiri.feature.place.presentation.PlaceScreen
import com.kiri.feature.record.presentation.RecordScreen

@Composable
fun KiriNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNavigateToLogin: () -> Unit = {},
) {
    NavHost(navController, startDestination = TopLevelDestination.HOME.graphRoute, modifier = modifier) {
        homeNavHost(navController, onOpenPlaces = { navController.navigate(TopLevelDestination.PLACE.graphRoute) })
        recordNavHost(navController)
        navigation(startDestination = TopLevelDestination.PLACE.startRoute, route = TopLevelDestination.PLACE.graphRoute) {
            composable(TopLevelDestination.PLACE.startRoute) {
                PlaceScreen(onDetailClick = { navController.navigate(TopLevelDestination.PLACE.detailRoute()) })
            }
            composable(TopLevelDestination.PLACE.detailRoute()) {
                DetailScreen(title = "장소 상세", onBackClick = navController::popBackStack)
            }
        }
        mypageGraph(navController, onNavigateToLogin = onNavigateToLogin)
    }
}
