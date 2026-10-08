package com.kiri.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.kiri.feature.home.navigation.homeNavHost
import com.kiri.feature.record.navigation.recordNavHost
import com.kiri.feature.place.presentation.PlaceScreen
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.kiri.feature.mypage.presentation.MypageScreen
import com.kiri.feature.detail.presentation.DetailScreen

@Composable
fun KiriNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
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
        navigation(startDestination = TopLevelDestination.MY_PAGE.startRoute, route = TopLevelDestination.MY_PAGE.graphRoute) {
            composable(TopLevelDestination.MY_PAGE.startRoute) {
                MypageScreen(onDetailClick = { navController.navigate(TopLevelDestination.MY_PAGE.detailRoute()) })
            }
            composable(TopLevelDestination.MY_PAGE.detailRoute()) {
                DetailScreen(title = "마이페이지 상세", onBackClick = navController::popBackStack)
            }
        }
    }
}
