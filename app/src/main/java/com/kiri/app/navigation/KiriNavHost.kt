package com.kiri.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.kiri.feature.detail.presentation.MissionDetailRoute
import com.kiri.feature.detail.presentation.ConditionCustomRoute
import com.kiri.feature.detail.presentation.DetailViewModel
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.kiri.feature.detail.presentation.DetailScreen
import com.kiri.feature.home.presentation.HomeScreen
import com.kiri.feature.home.presentation.HomeUiDestination
import com.kiri.feature.home.presentation.InviteFamilyScreen
import android.content.Intent
import com.kiri.feature.mypage.presentation.MypageScreen
import com.kiri.feature.place.presentation.PlaceScreen
import com.kiri.feature.record.presentation.RecordScreen

@Composable
fun KiriNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController, startDestination = TopLevelDestination.HOME.graphRoute, modifier = modifier) {
        navigation(startDestination = TopLevelDestination.HOME.startRoute, route = TopLevelDestination.HOME.graphRoute) {
            composable(TopLevelDestination.HOME.startRoute) {
                HomeScreen(
                    onMissionClick = { id -> navController.navigate("${TopLevelDestination.HOME.detailRoute()}/${Uri.encode(id)}") { launchSingleTop = true } },
                    onScheduleClick = { id -> navController.navigate("${TopLevelDestination.HOME.detailRoute()}/${Uri.encode(id)}") { launchSingleTop = true } },
                    onDestination = { destination -> if (destination == HomeUiDestination.OpenPlaces) navController.navigate(TopLevelDestination.PLACE.graphRoute) },
                    onInviteFamily = { code -> navController.navigate("home/invite/$code") },
                )
            }
            composable("home/invite/{inviteCode}") { backStackEntry ->
                val context = LocalContext.current
                val inviteCode = backStackEntry.arguments?.getString("inviteCode").orEmpty()
                InviteFamilyScreen(
                    inviteCode = inviteCode,
                    onBack = navController::popBackStack,
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "끼리네 가족에 참여해 주세요! 초대코드: $inviteCode")
                        }
                        context.startActivity(Intent.createChooser(intent, "가족에게 공유하기"))
                    },
                )
            }
            composable("${TopLevelDestination.HOME.detailRoute()}/{missionId}") { entry ->
                MissionDetailRoute(onBack = { navController.popBackStack() }, onCustom = {
                    val id = entry.arguments?.getString("missionId").orEmpty()
                    navController.navigate("${TopLevelDestination.HOME.detailRoute()}/${Uri.encode(id)}/custom") { launchSingleTop = true }
                })
            }
            composable("${TopLevelDestination.HOME.detailRoute()}/{missionId}/custom") { entry ->
                val parent = remember(entry) { navController.getBackStackEntry("${TopLevelDestination.HOME.detailRoute()}/{missionId}") }
                val viewModel: DetailViewModel = hiltViewModel(parent)
                ConditionCustomRoute(viewModel, onBack = { navController.popBackStack() })
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
