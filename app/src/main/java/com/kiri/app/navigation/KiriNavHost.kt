package com.kiri.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.kiri.feature.home.navigation.homeNavHost
import com.kiri.feature.home.navigation.serverMissionDetailRoute
import com.kiri.feature.record.navigation.recordNavHost
import com.kiri.feature.record.navigation.serverScheduleDetailRoute
import com.kiri.feature.place.navigation.placeNavHost
import com.kiri.feature.mypage.navigation.mypageGraph

@Composable
fun KiriNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNavigateToLogin: () -> Unit = {},
    onCreateFamily: () -> Unit = {},
    onJoinFamily: () -> Unit = {},
    onHomeAnswerEditingChanged: (Boolean) -> Unit = {},
) {
    NavHost(navController, startDestination = TopLevelDestination.HOME.graphRoute, modifier = modifier) {
        homeNavHost(
            navController,
            onOpenPlaces = { navController.navigateToTab(TopLevelDestination.PLACE) },
            onCreateFamily = onCreateFamily,
            onJoinFamily = onJoinFamily,
            onAnswerEditingChanged = onHomeAnswerEditingChanged,
            // 서버 일정 id 가 아니면 열 화면이 없다
            onSchedule = { id ->
                id.toLongOrNull()?.let { navController.navigate(serverScheduleDetailRoute(it, fromHome = true)) { launchSingleTop = true } }
            },
            // 응답이 필요한 일정 카드는 응답 화면으로 연다
            onRespondSchedule = { id ->
                id.toLongOrNull()?.let {
                    navController.navigate(serverScheduleDetailRoute(it, fromHome = true, respond = true)) { launchSingleTop = true }
                }
            },
            onTasteSettings = { navController.navigate("mypage_graph/my_preference") { launchSingleTop = true } },
            onFamilyInvite = { navController.navigate("mypage_graph/invite_link") { launchSingleTop = true } },
        )
        recordNavHost(
            navController,
            onNearbyMission = { id -> navController.navigate(serverMissionDetailRoute(id)) { launchSingleTop = true } },
        )
        placeNavHost(navController)
        mypageGraph(navController, onNavigateToLogin = onNavigateToLogin)
    }
}
