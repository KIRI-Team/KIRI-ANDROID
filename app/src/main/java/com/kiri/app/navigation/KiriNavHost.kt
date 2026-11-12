package com.kiri.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.kiri.feature.home.navigation.homeNavHost
import com.kiri.feature.record.navigation.recordNavHost
import com.kiri.feature.record.navigation.serverScheduleDetailRoute
import com.kiri.feature.place.navigation.placeNavHost
import com.kiri.feature.mypage.navigation.mypageGraph
import com.kiri.feature.schedule.*

@Composable
fun KiriNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNavigateToLogin: () -> Unit = {},
    onCreateFamily: () -> Unit = {},
    onJoinFamily: () -> Unit = {},
) {
    NavHost(navController, startDestination = TopLevelDestination.HOME.graphRoute, modifier = modifier) {
        homeNavHost(
            navController,
            onOpenPlaces = { navController.navigate(TopLevelDestination.PLACE.graphRoute) },
            onCreateFamily = onCreateFamily,
            onJoinFamily = onJoinFamily,
            onSchedule = { id ->
                val route = id.toLongOrNull()?.let { serverScheduleDetailRoute(it, fromHome = true) }
                    ?: scheduleRoute(id, "home_graph")
                navController.navigate(route) { launchSingleTop = true }
            },
            onTasteSettings = { navController.navigate("mypage_graph/my_preference") { launchSingleTop = true } },
            onFamilyInvite = { navController.navigate("mypage_graph/invite_link") { launchSingleTop = true } },
            scheduleDestinations = { scheduleGraph(navController, "home_graph") },
        )
        recordNavHost(navController, onCreateSchedule = { navController.navigate(scheduleCreateRoute(it, "record_graph")) }, onManageSchedule = { navController.navigate(scheduleRoute(it, "record_graph")) }, scheduleDestinations = { scheduleGraph(navController, "record_graph") })
        placeNavHost(navController)
        mypageGraph(navController, onNavigateToLogin = onNavigateToLogin)
    }
}
