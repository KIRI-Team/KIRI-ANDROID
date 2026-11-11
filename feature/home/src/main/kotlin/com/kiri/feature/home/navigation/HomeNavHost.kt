package com.kiri.feature.home.navigation

import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.kiri.feature.detail.presentation.DetailScreen
import android.net.Uri
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.kiri.feature.detail.DetailViewModel
import com.kiri.feature.detail.presentation.MissionDetailRoute
import com.kiri.feature.detail.presentation.MissionPreviewRoute
import com.kiri.feature.detail.presentation.ConditionCustomRoute
import com.kiri.feature.home.presentation.HomeRoute
import com.kiri.feature.home.presentation.InviteFamilyScreen
import com.kiri.feature.home.model.HomeUiDestination
import com.kiri.feature.home.HomeViewModel
import com.kiri.feature.home.model.HomeUiEvent
import com.kiri.feature.home.presentation.NotificationRoute
import com.kiri.domain.notification.NotificationTarget
import com.kiri.feature.record.presentation.MemoryEditorScreen
import com.kiri.feature.record.ScheduleRegisterViewModel
import com.kiri.feature.record.presentation.schedule.ScheduleRegisterRoute

private const val GRAPH_ROUTE = "home_graph"
private const val START_ROUTE = "home"
private const val DETAIL_ROUTE = "home_graph/detail"

/** Registers this feature in the main tab back stack. */
fun NavGraphBuilder.homeNavHost(navController: NavHostController, onOpenPlaces: () -> Unit, onSchedule: (String) -> Unit = {}, onTasteSettings: () -> Unit = {}, onFamilyInvite: () -> Unit = {}, scheduleDestinations: NavGraphBuilder.() -> Unit = {}) {
    navigation(startDestination = START_ROUTE, route = GRAPH_ROUTE) {
        scheduleDestinations()
        composable("home_graph/recommendation/{missionId}") {
            MissionPreviewRoute(onBack = { navController.popBackStack() })
        }
        composable("home_graph/weekly-schedule/{weeklyMissionId}") { entry ->
            val weeklyId = entry.arguments?.getString("weeklyMissionId").orEmpty()
            if (weeklyId.toLongOrNull() != null) {
                val viewModel: com.kiri.feature.record.BucketScheduleCreateViewModel = hiltViewModel(entry)
                com.kiri.feature.record.presentation.schedule.BucketScheduleCreateRoute(viewModel,
                    onBackClick = { navController.popBackStack() }, onSentByApp = { navController.popBackStack() }, onSentByKakao = { navController.popBackStack() })
            } else {
                val viewModel: ScheduleRegisterViewModel = hiltViewModel(entry)
                ScheduleRegisterRoute(viewModel, onBackClick = { navController.popBackStack() },
                    onDetailClick = { navController.navigate("$DETAIL_ROUTE/${Uri.encode(weeklyId)}") },
                    onBucketMissing = { navController.popBackStack() }, onSentByApp = { navController.popBackStack() }, onSentByKakao = { navController.popBackStack() })
            }
        }
        composable(START_ROUTE) {
            HomeRoute(
                onMissionClick = { id ->
                    val route = if (id.toLongOrNull() != null) "home_graph/recommendation/$id"
                    else "$DETAIL_ROUTE/${Uri.encode(id)}"
                    navController.navigate(route) { launchSingleTop = true }
                },
                onScheduleClick = onSchedule,
                onWeeklyScheduleClick = { id ->
                    navController.navigate("home_graph/weekly-schedule/${Uri.encode(id)}") { launchSingleTop = true }
                },
                onDestination = { destination ->
                    when (destination) {
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
                    NotificationTarget.QUESTION -> {
                        if (homeViewModel.uiState.value.home?.question?.id == notification.targetId) homeViewModel.onEvent(HomeUiEvent.AnswerEditClicked)
                        navController.popBackStack()
                    }
                    NotificationTarget.SCHEDULE -> onSchedule(notification.targetId)
                    else -> navController.navigate("$DETAIL_ROUTE/${Uri.encode(notification.targetId)}?memories=${notification.target == NotificationTarget.MEMORY}") { launchSingleTop = true }
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
        composable("${DETAIL_ROUTE}/{missionId}?memories={memories}", arguments = listOf(androidx.navigation.navArgument("memories") { defaultValue = "false" })) { entry ->
            val viewModel: DetailViewModel = hiltViewModel(entry)
            MissionDetailRoute(onBack = { navController.popBackStack() }, onCustom = {
                val id = entry.arguments?.getString("missionId").orEmpty()
                navController.navigate("${DETAIL_ROUTE}/${Uri.encode(id)}/custom") { launchSingleTop = true }
            }, viewModel = viewModel, onRecord = {
                val id = entry.arguments?.getString("missionId").orEmpty()
                navController.navigate("$DETAIL_ROUTE/${Uri.encode(id)}/record") { launchSingleTop = true }
            })
        }
        composable("${DETAIL_ROUTE}/{missionId}/record") { entry ->
            val parent = remember(entry) { navController.getBackStackEntry("${DETAIL_ROUTE}/{missionId}?memories={memories}") }
            val viewModel: DetailViewModel = hiltViewModel(parent)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            MemoryEditorScreen(state.memoryDraft, state.detail?.participants?.firstOrNull()?.name ?: "나",
                onChange = viewModel::updateMemoryDraft, onBack = { navController.popBackStack() },
                onSubmit = { viewModel.submitMemory { navController.popBackStack() } }, saving = state.saving, error = state.error)
        }
        composable("${DETAIL_ROUTE}/{missionId}/custom") { entry ->
            val parent = remember(entry) { navController.getBackStackEntry("${DETAIL_ROUTE}/{missionId}?memories={memories}") }
            val viewModel: DetailViewModel = hiltViewModel(parent)
            ConditionCustomRoute(viewModel, onBack = { navController.popBackStack() })
        }
    }
}
