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
import com.kiri.feature.detail.presentation.ConditionCustomRoute
import com.kiri.feature.home.presentation.HomeRoute
import com.kiri.feature.home.presentation.InviteFamilyScreen
import com.kiri.feature.home.model.HomeUiDestination
import com.kiri.feature.home.HomeViewModel
import com.kiri.feature.home.model.HomeUiEvent
import com.kiri.feature.home.presentation.NotificationRoute
import com.kiri.domain.notification.NotificationTarget
import com.kiri.feature.record.presentation.MemoryEditorScreen

private const val GRAPH_ROUTE = "home_graph"
private const val START_ROUTE = "home"
private const val DETAIL_ROUTE = "home_graph/detail"

/** Registers this feature in the main tab back stack. */
fun NavGraphBuilder.homeNavHost(navController: NavHostController, onOpenPlaces: () -> Unit) {
    navigation(startDestination = START_ROUTE, route = GRAPH_ROUTE) {
        composable(START_ROUTE) {
            HomeRoute(
                onMissionClick = { id -> navController.navigate("${DETAIL_ROUTE}/${Uri.encode(id)}") { launchSingleTop = true } },
                onScheduleClick = { id -> navController.navigate("${DETAIL_ROUTE}/${Uri.encode(id)}") { launchSingleTop = true } },
                onDestination = { destination ->
                    when (destination) {
                        HomeUiDestination.OpenPlaces -> onOpenPlaces()
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
                onSubmit = { if (viewModel.submitMemoryPreview()) navController.popBackStack() })
        }
        composable("${DETAIL_ROUTE}/{missionId}/custom") { entry ->
            val parent = remember(entry) { navController.getBackStackEntry("${DETAIL_ROUTE}/{missionId}?memories={memories}") }
            val viewModel: DetailViewModel = hiltViewModel(parent)
            ConditionCustomRoute(viewModel, onBack = { navController.popBackStack() })
        }
    }
}
