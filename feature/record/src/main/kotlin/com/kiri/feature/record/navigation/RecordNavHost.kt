package com.kiri.feature.record.navigation

import android.net.Uri
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import com.kiri.feature.detail.presentation.DetailScreen
import com.kiri.feature.record.presentation.RecordRoute
import com.kiri.feature.record.presentation.*
import com.kiri.feature.record.RecordDetailViewModel
import com.kiri.feature.record.RecordViewModel
import com.kiri.feature.record.ScheduleRegisterViewModel
import com.kiri.feature.record.ServerScheduleDetailViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.kiri.feature.record.presentation.schedule.ScheduleRegisterRoute
import com.kiri.feature.record.presentation.schedule.ScheduleSentToast
import com.kiri.feature.record.presentation.schedule.savedByLabel
import com.kiri.feature.record.presentation.sheet.RecordItemOptionSheet
import com.kiri.feature.detail.presentation.ConditionCustomScreen
import com.kiri.feature.detail.model.DetailUiEvent

private const val GRAPH_ROUTE = "record_graph"
private const val START_ROUTE = "record"
private const val DETAIL_ROUTE = "record_graph/detail"
private const val SCHEDULE_ROUTE = "record_graph/schedule"
private const val SERVER_SCHEDULE_ROUTE = "record_graph/server-schedule"
private const val SENT_TOAST_KEY = "scheduleSentToast"

/** Registers this feature in the main tab back stack. */
fun NavGraphBuilder.recordNavHost(navController: NavHostController, onCreateSchedule: (String) -> Unit = {}, onManageSchedule: (String) -> Unit = {}, scheduleDestinations: NavGraphBuilder.() -> Unit = {}) {
    navigation(startDestination = START_ROUTE, route = GRAPH_ROUTE) {
        scheduleDestinations()
        composable(START_ROUTE) { entry ->
            val viewModel: RecordViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            // 버킷 카드를 누르면 일정 잡기·삭제하기 시트를 먼저 띄운다
            var optionBucketId by rememberSaveable { mutableStateOf<String?>(null) }
            val showSentToast by entry.savedStateHandle.getStateFlow(SENT_TOAST_KEY, false).collectAsStateWithLifecycle()
            Box(Modifier.fillMaxSize()) {
                RecordRoute(
                    onScheduleClick = onManageSchedule,
                    onBucketClick = { optionBucketId = it },
                    onEntryClick = { navController.navigate("$DETAIL_ROUTE/${Uri.encode(it)}") { launchSingleTop = true } },
                    viewModel = viewModel,
                    onServerScheduleClick = { navController.navigate("$SERVER_SCHEDULE_ROUTE/$it") { launchSingleTop = true } },
                )
                if (showSentToast) {
                    ScheduleSentToast(onDismiss = { entry.savedStateHandle[SENT_TOAST_KEY] = false }, modifier = Modifier.align(Alignment.TopCenter))
                }
            }
            state.buckets.find { it.id == optionBucketId }?.let { bucket ->
                val activity = state.entries.find { it.id == bucket.id }
                val linkedSchedules = activity?.linkedScheduleIds.orEmpty()
                val completed = activity?.completed == true
                RecordItemOptionSheet(
                    title = bucket.title,
                    subtitle = savedByLabel(state.saverName(bucket.saverId)),
                    scheduleLabel = if (completed) "완료한 활동" else if (linkedSchedules.isEmpty()) "일정 잡기" else "연결된 약속 보기",
                    scheduleEnabled = !completed,
                    deleteEnabled = activity?.saverId == state.currentUserId && !completed && linkedSchedules.isEmpty(),
                    onScheduleClick = {
                        optionBucketId = null
                        if (linkedSchedules.isNotEmpty()) onManageSchedule(linkedSchedules.first())
                        else if (!completed) navController.navigate("$SCHEDULE_ROUTE/${Uri.encode(bucket.id)}") { launchSingleTop = true }
                    },
                    onDeleteClick = {
                        optionBucketId = null
                        navController.navigate("$DETAIL_ROUTE/${Uri.encode(bucket.id)}") { launchSingleTop = true }
                    },
                    onDismiss = { optionBucketId = null },
                )
            }
        }
        composable("$SCHEDULE_ROUTE/{entryId}") { entry ->
            val viewModel: ScheduleRegisterViewModel = hiltViewModel(entry)
            val id = entry.arguments?.getString("entryId").orEmpty()
            ScheduleRegisterRoute(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onDetailClick = { navController.navigate("$DETAIL_ROUTE/${Uri.encode(id)}") { launchSingleTop = true } },
                // 이 화면 위에 다른 화면이 쌓여 있어도 이 화면까지 한 번만 걷어낸다
                onBucketMissing = { navController.popBackStack("$SCHEDULE_ROUTE/{entryId}", inclusive = true) },
                // 요청이 끝나면 버킷이 빠진 기록 탭으로 돌아간다. 앱으로 보냈을 때만 발송 완료 알림을 띄운다
                onSentByApp = {
                    navController.getBackStackEntry(START_ROUTE).savedStateHandle[SENT_TOAST_KEY] = true
                    navController.popBackStack(START_ROUTE, inclusive = false)
                },
                onSentByKakao = { navController.popBackStack(START_ROUTE, inclusive = false) },
            )
        }
        // 캘린더에서 고른 서버 일정의 상세. 내용은 일정 상세 API 로 채운다
        composable(
            "$SERVER_SCHEDULE_ROUTE/{${ServerScheduleDetailViewModel.SCHEDULE_ID_ARG}}",
            arguments = listOf(navArgument(ServerScheduleDetailViewModel.SCHEDULE_ID_ARG) { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong(ServerScheduleDetailViewModel.SCHEDULE_ID_ARG)
            ServerScheduleDetailRoute(
                hiltViewModel(entry),
                onBack = { navController.popBackStack() },
                onCustom = { navController.navigate("$SERVER_SCHEDULE_ROUTE/$id/custom") { launchSingleTop = true } },
            )
        }
        // 약속 당일의 컨디션 커스텀. 상세 화면의 ViewModel 을 함께 쓴다. 적용 API 가 없어 적용하기는 꺼 둔다
        composable("$SERVER_SCHEDULE_ROUTE/{${ServerScheduleDetailViewModel.SCHEDULE_ID_ARG}}/custom") { entry ->
            val parent = remember(entry) {
                navController.getBackStackEntry("$SERVER_SCHEDULE_ROUTE/{${ServerScheduleDetailViewModel.SCHEDULE_ID_ARG}}")
            }
            val viewModel: ServerScheduleDetailViewModel = hiltViewModel(parent)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ServerScheduleDetailEffects(viewModel, onBack = { navController.popBackStack() })
            ConditionCustomScreen(state.detail, viewModel::onEvent, Modifier.padding(bottom = 110.dp), applyEnabled = false)
        }
        composable(DETAIL_ROUTE) {
            DetailScreen(title = "기록 상세", onBackClick = navController::popBackStack)
        }
        composable("$DETAIL_ROUTE/{entryId}") { entry ->
            val viewModel: RecordDetailViewModel = hiltViewModel(entry)
            val id = Uri.encode(entry.arguments?.getString("entryId").orEmpty())
            RecordDetailRoute(viewModel, onBack = { navController.popBackStack() },
                onCustom = { navController.navigate("$DETAIL_ROUTE/$id/custom") { launchSingleTop = true } },
                onPhoto = { navController.navigate("$DETAIL_ROUTE/$id/photos") { launchSingleTop = true } },
                onEdit = { navController.navigate("$DETAIL_ROUTE/$id/edit") { launchSingleTop = true } },
                onCreateSchedule = { navController.navigate("$SCHEDULE_ROUTE/${Uri.encode(it)}") { launchSingleTop = true } }, onManageSchedule = onManageSchedule)
        }
        composable("$DETAIL_ROUTE/{entryId}/custom") { entry ->
            val parent = remember(entry) { navController.getBackStackEntry("$DETAIL_ROUTE/{entryId}") }
            val viewModel: RecordDetailViewModel = hiltViewModel(parent)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            RecordDetailEffects(viewModel, onBack = { navController.popBackStack() }, returnToDetail = true)
            ConditionCustomScreen(state.detail, viewModel::onDetailEvent, Modifier.padding(bottom = 110.dp))
        }
        dialog("$DETAIL_ROUTE/{entryId}/photos", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { entry ->
            val parent = remember(entry) { navController.getBackStackEntry("$DETAIL_ROUTE/{entryId}") }
            val viewModel: RecordDetailViewModel = hiltViewModel(parent)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            RecordDetailEffects(viewModel, onBack = { navController.popBackStack() }, returnToDetail = true)
            RecordPhotoSelectionScreen(state, onBack = { navController.popBackStack() }, viewModel::selectPhoto, viewModel::savePhoto)
        }
        composable("$DETAIL_ROUTE/{entryId}/edit") { entry ->
            val parent = remember(entry) { navController.getBackStackEntry("$DETAIL_ROUTE/{entryId}") }
            val viewModel: RecordDetailViewModel = hiltViewModel(parent)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            RecordDetailEffects(viewModel, onBack = { navController.popBackStack() }, returnToDetail = true)
            MemoryEditorScreen(state.memoryDraft, state.collection.memories.find { it.id == state.editingMemoryId }?.authorName.orEmpty(),
                onChange = viewModel::updateDraft, onBack = { navController.popBackStack() }, onSubmit = viewModel::saveMemory,
                editing = true, saving = state.detail.saving, error = state.detail.error)
        }
    }
}
