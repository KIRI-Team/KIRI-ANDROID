package com.kiri.feature.home.navigation

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import android.net.Uri
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.kiri.feature.detail.presentation.MissionPreviewRoute
import com.kiri.feature.home.presentation.HomeRoute
import com.kiri.feature.home.presentation.InviteFamilyScreen
import com.kiri.feature.home.model.HomeUiDestination
import com.kiri.feature.home.HomeViewModel
import com.kiri.feature.home.model.HomeUiEvent
import com.kiri.feature.home.presentation.NotificationRoute
import com.kiri.domain.notification.NotificationTarget

private const val GRAPH_ROUTE = "home_graph"
private const val START_ROUTE = "home"
private const val RECOMMENDATION_ROUTE = "home_graph/recommendation"

fun serverMissionDetailRoute(missionId: Long): String = "$RECOMMENDATION_ROUTE/$missionId"

/** Registers this feature in the main tab back stack. */
fun NavGraphBuilder.homeNavHost(navController: NavHostController, onOpenPlaces: () -> Unit, onSchedule: (String) -> Unit = {}, onRespondSchedule: (String) -> Unit = onSchedule, onTasteSettings: () -> Unit = {}, onFamilyInvite: () -> Unit = {}, onCreateFamily: () -> Unit = {}, onJoinFamily: () -> Unit = {}, onAnswerEditingChanged: (Boolean) -> Unit = {}) {
    navigation(startDestination = START_ROUTE, route = GRAPH_ROUTE) {
        composable("$RECOMMENDATION_ROUTE/{missionId}") {
            MissionPreviewRoute(
                onBack = { navController.popBackStack() },
                onNearbyMission = { id -> navController.navigate(serverMissionDetailRoute(id)) { launchSingleTop = true } },
            )
        }
        composable(START_ROUTE) {
            HomeRoute(
                onAnswerEditingChanged = onAnswerEditingChanged,
                // 서버 미션 id 가 아니면 열 화면이 없다
                onMissionClick = { id ->
                    id.toLongOrNull()?.let { navController.navigate(serverMissionDetailRoute(it)) { launchSingleTop = true } }
                },
                onScheduleClick = onSchedule,
                onRespondSchedule = onRespondSchedule,
                onDestination = { destination ->
                    when (destination) {
                        HomeUiDestination.CreateFamily -> onCreateFamily()
                        HomeUiDestination.JoinFamily -> onJoinFamily()
                        HomeUiDestination.OpenPlaces -> onOpenPlaces()
                        HomeUiDestination.TasteSettings -> onTasteSettings()
                        HomeUiDestination.InviteFamily -> onFamilyInvite()
                        HomeUiDestination.Notifications -> navController.navigate("home/notifications") { launchSingleTop = true }
                        else -> Unit
                    }
                },
                onInviteFamily = { code -> navController.navigate("home/invite/${Uri.encode(code)}") },
            )
        }
        composable("home/notifications") {
            val homeEntry = remember { navController.getBackStackEntry(START_ROUTE) }
            val homeViewModel: HomeViewModel = hiltViewModel(homeEntry)
            NotificationRoute(onBack = { navController.popBackStack() }, onTarget = { notification ->
                when (notification.target) {
                    NotificationTarget.QUESTION, NotificationTarget.FAMILY_ANSWERS -> {
                        if (homeViewModel.uiState.value.home?.question?.id != notification.targetId) {
                            false
                        } else {
                            if (notification.target == NotificationTarget.QUESTION) {
                                homeViewModel.onEvent(HomeUiEvent.AnswerEditClicked)
                            }
                            navController.popBackStack()
                        }
                    }
                    NotificationTarget.SCHEDULE_INVITATION -> {
                        onRespondSchedule(notification.targetId)
                        true
                    }
                    NotificationTarget.SCHEDULE, NotificationTarget.MEMORY -> {
                        onSchedule(notification.targetId)
                        true
                    }
                    NotificationTarget.MISSION -> {
                        val id = notification.targetId.toLongOrNull()
                        if (id == null) false else {
                            navController.navigate(serverMissionDetailRoute(id)) { launchSingleTop = true }
                            true
                        }
                    }
                    NotificationTarget.UNSUPPORTED -> false
                }
            })
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
    }
}
