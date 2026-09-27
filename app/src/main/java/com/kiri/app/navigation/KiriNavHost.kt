package com.kiri.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.kiri.feature.detail.presentation.DetailScreen
import com.kiri.feature.home.presentation.HomeScreen
import com.kiri.feature.mypage.presentation.MypageScreen
import com.kiri.feature.place.presentation.PlaceScreen
import com.kiri.feature.record.presentation.RecordScreen

@Composable
fun KiriNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController, startDestination = TopLevelDestination.HOME.graphRoute, modifier = modifier) {
        navigation(startDestination = TopLevelDestination.HOME.startRoute, route = TopLevelDestination.HOME.graphRoute) {
            composable(TopLevelDestination.HOME.startRoute) {
                HomeScreen(onDetailClick = { navController.navigate(TopLevelDestination.HOME.detailRoute()) })
            }
            composable(TopLevelDestination.HOME.detailRoute()) {
                DetailScreen(title = "홈 상세", onBackClick = navController::popBackStack)
            }
        }
        navigation(startDestination = TopLevelDestination.RECORD.startRoute, route = TopLevelDestination.RECORD.graphRoute) {
            composable(TopLevelDestination.RECORD.startRoute) {
                RecordScreen(onDetailClick = { navController.navigate(TopLevelDestination.RECORD.detailRoute()) })
            }
            composable(TopLevelDestination.RECORD.detailRoute()) {
                DetailScreen(title = "기록 상세", onBackClick = navController::popBackStack)
            }
        }
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
