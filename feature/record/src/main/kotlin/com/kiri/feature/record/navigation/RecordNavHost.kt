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
import com.kiri.feature.record.BucketScheduleCreateViewModel
import com.kiri.feature.record.presentation.schedule.BucketScheduleCreateRoute
import com.kiri.feature.record.presentation.bucket.options
import com.kiri.uikit.component.KiriToast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.kiri.feature.record.model.RecordUiEvent
import com.kiri.feature.record.ScheduleEditViewModel
import com.kiri.feature.record.presentation.schedule.ScheduleEditRoute
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
private const val SCHEDULE_EDIT_ROUTE = "record_graph/schedule-edit"
private const val BUCKET_SCHEDULE_ROUTE = "record_graph/bucket-schedule"
private const val SERVER_SCHEDULE_DETAIL_PATTERN =
    "$SERVER_SCHEDULE_ROUTE/{${ServerScheduleDetailViewModel.SCHEDULE_ID_ARG}}?${ServerScheduleDetailViewModel.DATE_ARG}={${ServerScheduleDetailViewModel.DATE_ARG}}"
private const val SENT_TOAST_KEY = "scheduleSentToast"
// 일정 변경 화면에서 저장했음을 상세 화면에 알린다
private const val SCHEDULE_EDITED_KEY = "scheduleEdited"

fun serverScheduleDetailRoute(scheduleId: Long): String = "$SERVER_SCHEDULE_ROUTE/$scheduleId"

/** Registers this feature in the main tab back stack. */
fun NavGraphBuilder.recordNavHost(navController: NavHostController, onCreateSchedule: (String) -> Unit = {}, onManageSchedule: (String) -> Unit = {}, scheduleDestinations: NavGraphBuilder.() -> Unit = {}) {
    navigation(startDestination = START_ROUTE, route = GRAPH_ROUTE) {
        scheduleDestinations()
        composable(START_ROUTE) { entry ->
            val viewModel: RecordViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            // 버킷 카드를 누르면 일정 잡기·삭제하기 시트를 먼저 띄운다
            var optionBucketId by rememberSaveable { mutableStateOf<String?>(null) }
            // 아직 연결되지 않은 버킷 동작을 눌렀을 때 잠깐 띄우는 안내
            var notice by rememberSaveable { mutableStateOf<String?>(null) }
            // 삭제하기를 누른 버킷. 확인 창에서 한 번 더 확인한 뒤 지운다
            var confirmDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
            val showSentToast by entry.savedStateHandle.getStateFlow(SENT_TOAST_KEY, false).collectAsStateWithLifecycle()
            Box(Modifier.fillMaxSize()) {
                RecordRoute(
                    onScheduleClick = onManageSchedule,
                    onBucketClick = { optionBucketId = it },
                    onEntryClick = { navController.navigate("$DETAIL_ROUTE/${Uri.encode(it)}") { launchSingleTop = true } },
                    viewModel = viewModel,
                    onServerScheduleClick = { id, date ->
                        navController.navigate("$SERVER_SCHEDULE_ROUTE/$id?${ServerScheduleDetailViewModel.DATE_ARG}=$date") { launchSingleTop = true }
                    },
                    onNotice = { notice = it },
                )
                if (showSentToast) {
                    ScheduleSentToast(onDismiss = { entry.savedStateHandle[SENT_TOAST_KEY] = false }, modifier = Modifier.align(Alignment.TopCenter))
                }
                notice?.let { message ->
                    LaunchedEffect(message) {
                        delay(2_000)
                        notice = null
                    }
                    KiriToast(message, Modifier.align(Alignment.TopCenter).padding(top = 56.dp, start = 18.dp, end = 18.dp).testTag("record_notice_toast"))
                }
            }
            // 서버 버킷: 저장 상태면 일정 등록 화면으로, 일정이 잡힌 버킷은 연결된 일정 상세로 간다. 삭제는 확인 창을 거친다
            state.bucketItems.find { it.id.toString() == optionBucketId }?.let { bucket ->
                val options = bucket.options()
                RecordItemOptionSheet(
                    title = bucket.title,
                    subtitle = savedByLabel(bucket.saverNickname),
                    scheduleLabel = options.scheduleLabel,
                    scheduleEnabled = options.scheduleEnabled,
                    deleteEnabled = options.deleteEnabled,
                    onScheduleClick = {
                        optionBucketId = null
                        val scheduleId = options.linkedScheduleId
                        if (scheduleId != null) {
                            val date = bucket.scheduledDate?.let { "?${ServerScheduleDetailViewModel.DATE_ARG}=$it" }.orEmpty()
                            navController.navigate("$SERVER_SCHEDULE_ROUTE/$scheduleId$date") { launchSingleTop = true }
                        } else {
                            val args = BucketScheduleCreateViewModel
                            navController.navigate(
                                "$BUCKET_SCHEDULE_ROUTE/${bucket.id}?${args.TITLE_ARG}=${Uri.encode(bucket.title)}&${args.SAVER_ARG}=${Uri.encode(bucket.saverNickname)}",
                            ) { launchSingleTop = true }
                        }
                    },
                    onDeleteClick = {
                        optionBucketId = null
                        confirmDeleteId = bucket.id.toString()
                    },
                    onDismiss = { optionBucketId = null },
                )
            }
            confirmDeleteId?.let { id ->
                AlertDialog(
                    onDismissRequest = { confirmDeleteId = null },
                    title = { Text("버킷을 삭제할까요?") },
                    confirmButton = {
                        TextButton({
                            confirmDeleteId = null
                            viewModel.onEvent(RecordUiEvent.BucketDeleteConfirmed(id))
                        }, Modifier.testTag("record_bucket_delete_confirm")) { Text("삭제하기") }
                    },
                    dismissButton = { TextButton({ confirmDeleteId = null }) { Text("취소") } },
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
            SERVER_SCHEDULE_DETAIL_PATTERN,
            arguments = listOf(
                navArgument(ServerScheduleDetailViewModel.SCHEDULE_ID_ARG) { type = NavType.LongType },
                navArgument(ServerScheduleDetailViewModel.DATE_ARG) { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            val id = entry.arguments?.getLong(ServerScheduleDetailViewModel.SCHEDULE_ID_ARG)
            val detailViewModel: ServerScheduleDetailViewModel = hiltViewModel(entry)
            // 변경 화면에서 저장하고 돌아오면 바뀐 일정을 다시 불러온다
            val edited by entry.savedStateHandle.getStateFlow(SCHEDULE_EDITED_KEY, false).collectAsStateWithLifecycle()
            LaunchedEffect(edited) {
                if (edited) {
                    entry.savedStateHandle[SCHEDULE_EDITED_KEY] = false
                    detailViewModel.reload()
                }
            }
            ServerScheduleDetailRoute(
                detailViewModel,
                onBack = { navController.popBackStack() },
                onCustom = { navController.navigate("$SERVER_SCHEDULE_ROUTE/$id/custom") { launchSingleTop = true } },
                // 상세 위에 변경 화면을 띄워, 뒤로가기나 저장 후에는 상세로 돌아온다
                onEdit = { scheduleId ->
                    navController.navigate("$SCHEDULE_EDIT_ROUTE/$scheduleId") { launchSingleTop = true }
                },
            )
        }
        // 제안자의 일정 변경하기 (일정 등록 화면의 변경 모드). 내용은 일정 상세 API 로 채우고 일정 수정 API 로 저장한다
        composable(
            "$SCHEDULE_EDIT_ROUTE/{${ScheduleEditViewModel.SCHEDULE_ID_ARG}}",
            arguments = listOf(navArgument(ScheduleEditViewModel.SCHEDULE_ID_ARG) { type = NavType.LongType }),
        ) { entry ->
            ScheduleEditRoute(
                hiltViewModel(entry),
                onBackClick = { navController.popBackStack() },
                onSaved = {
                    navController.previousBackStackEntry?.savedStateHandle?.set(SCHEDULE_EDITED_KEY, true)
                    navController.popBackStack()
                },
            )
        }
        // 약속 당일의 컨디션 커스텀. 상세 화면의 ViewModel 을 함께 쓴다. 들어갈 때 지금 적용된 조건을 불러오고,
        // 적용하기로 고른 조건을 저장한 뒤 상세로 돌아간다
        composable("$SERVER_SCHEDULE_ROUTE/{${ServerScheduleDetailViewModel.SCHEDULE_ID_ARG}}/custom") { entry ->
            val parent = remember(entry) {
                navController.getBackStackEntry(SERVER_SCHEDULE_DETAIL_PATTERN)
            }
            val viewModel: ServerScheduleDetailViewModel = hiltViewModel(parent)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ServerScheduleDetailEffects(viewModel, onBack = { navController.popBackStack() })
            ConditionCustomScreen(
                state.detail.copy(error = state.customError),
                viewModel::onEvent,
                Modifier.padding(bottom = 110.dp),
                // 지금 조건을 불러오는 동안에는 적용하지 않는다
                applyEnabled = !state.customLoading && state.customLoaded,
            )
        }
        // 서버 버킷의 일정 잡기. 일정 생성 API 로 만들고, 다 보내면 버킷이 일정 잡힘으로 바뀐 기록 탭으로 돌아간다
        composable(
            "$BUCKET_SCHEDULE_ROUTE/{${BucketScheduleCreateViewModel.BUCKET_ITEM_ID_ARG}}" +
                "?${BucketScheduleCreateViewModel.TITLE_ARG}={${BucketScheduleCreateViewModel.TITLE_ARG}}" +
                "&${BucketScheduleCreateViewModel.SAVER_ARG}={${BucketScheduleCreateViewModel.SAVER_ARG}}",
            arguments = listOf(
                navArgument(BucketScheduleCreateViewModel.BUCKET_ITEM_ID_ARG) { type = NavType.LongType },
                navArgument(BucketScheduleCreateViewModel.TITLE_ARG) { type = NavType.StringType; defaultValue = "" },
                navArgument(BucketScheduleCreateViewModel.SAVER_ARG) { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            BucketScheduleCreateRoute(
                hiltViewModel(entry),
                onBackClick = { navController.popBackStack() },
                // 앱으로 보냈을 때만 발송 완료 알림을 띄운다
                onSentByApp = {
                    navController.getBackStackEntry(START_ROUTE).savedStateHandle[SENT_TOAST_KEY] = true
                    navController.popBackStack(START_ROUTE, inclusive = false)
                },
                onSentByKakao = { navController.popBackStack(START_ROUTE, inclusive = false) },
            )
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
