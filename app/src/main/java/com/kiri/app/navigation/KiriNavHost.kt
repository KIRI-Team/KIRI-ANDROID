package com.kiri.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.kiri.feature.home.navigation.homeNavHost
import com.kiri.feature.record.navigation.recordNavHost
import com.kiri.feature.place.navigation.placeNavHost
import com.kiri.feature.mypage.navigation.mypageGraph

@Composable
fun KiriNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNavigateToLogin: () -> Unit = {},
) {
    NavHost(navController, startDestination = TopLevelDestination.HOME.graphRoute, modifier = modifier) {
        homeNavHost(navController, onOpenPlaces = { navController.navigate(TopLevelDestination.PLACE.graphRoute) })
        recordNavHost(navController)
        placeNavHost(navController)
        mypageGraph(navController, onNavigateToLogin = onNavigateToLogin)
    }
}
