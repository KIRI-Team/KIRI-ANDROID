package com.kiri.feature.record.navigation

import com.kiri.feature.record.presentation.bucket.saverLabelOf
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
import com.kiri.feature.record.presentation.RecordRoute
import com.kiri.feature.record.presentation.*
import com.kiri.feature.record.RecordViewModel
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
import com.kiri.feature.record.presentation.schedule.ScheduleSentToast
import com.kiri.feature.record.presentation.schedule.savedByLabel
import com.kiri.feature.record.presentation.sheet.RecordItemOptionSheet
import com.kiri.feature.detail.presentation.ConditionCustomScreen

private const val GRAPH_ROUTE = "record_graph"
private const val START_ROUTE = "record"
private const val SERVER_SCHEDULE_ROUTE = "record_graph/server-schedule"
private const val SCHEDULE_EDIT_ROUTE = "record_graph/schedule-edit"
private const val BUCKET_SCHEDULE_ROUTE = "record_graph/bucket-schedule"
private const val SERVER_SCHEDULE_DETAIL_PATTERN =
    "$SERVER_SCHEDULE_ROUTE/{${ServerScheduleDetailViewModel.SCHEDULE_ID_ARG}}?${ServerScheduleDetailViewModel.DATE_ARG}={${ServerScheduleDetailViewModel.DATE_ARG}}&fromHome={fromHome}&respond={respond}"
private const val SENT_TOAST_KEY = "scheduleSentToast"
// 일정 변경 화면에서 저장했음을 상세 화면에 알린다
private const val SCHEDULE_EDITED_KEY = "scheduleEdited"

// respond: 홈의 응답 필요 카드로 연다. 응답할 수 있으면 상세 대신 응답 화면을 먼저 보여준다
fun serverScheduleDetailRoute(scheduleId: Long, fromHome: Boolean = false, respond: Boolean = false): String =
    "$SERVER_SCHEDULE_ROUTE/$scheduleId" + listOfNotNull("fromHome=true".takeIf { fromHome }, "respond=true".takeIf { respond })
        .joinToString("&").let { if (it.isEmpty()) it else "?$it" }

// 기록 탭(캘린더 날짜, 다가오는 약속 카드)에서 일정 상세를 연다. 기록 탭에서는 응답 화면으로 가지 않는다
internal fun calendarScheduleDetailRoute(scheduleId: Long, date: java.time.LocalDate): String =
    "$SERVER_SCHEDULE_ROUTE/$scheduleId?${ServerScheduleDetailViewModel.DATE_ARG}=$date"

/** Registers this feature in the main tab back stack. */
fun NavGraphBuilder.recordNavHost(navController: NavHostController, onNearbyMission: ((Long) -> Unit)? = null) {
    navigation(startDestination = START_ROUTE, route = GRAPH_ROUTE) {
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
                    onBucketClick = { optionBucketId = it },
                    viewModel = viewModel,
                    onServerScheduleClick = { id, date ->
                        navController.navigate(calendarScheduleDetailRoute(id, date)) { launchSingleTop = true }
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
                // 같은 미션을 여러 가족이 저장했으면 "엄마, 아빠가 저장했어요!" 처럼 함께 보여준다
                val savers = state.bucketItems.saverLabelOf(bucket)
                RecordItemOptionSheet(
                    title = bucket.title,
                    subtitle = savedByLabel(savers),
                    scheduleLabel = options.scheduleLabel,
                    scheduleEnabled = options.scheduleEnabled,
                    deleteEnabled = options.deleteEnabled,
                    imageUrl = bucket.imageUrl,
                    onScheduleClick = {
                        optionBucketId = null
                        val scheduleId = options.linkedScheduleId
                        if (scheduleId != null) {
                            val date = bucket.scheduledDate?.let { "?${ServerScheduleDetailViewModel.DATE_ARG}=$it" }.orEmpty()
                            navController.navigate("$SERVER_SCHEDULE_ROUTE/$scheduleId$date") { launchSingleTop = true }
                        } else {
                            val args = BucketScheduleCreateViewModel
                            navController.navigate(
                                "$BUCKET_SCHEDULE_ROUTE/${bucket.id}?${args.TITLE_ARG}=${Uri.encode(bucket.title)}&${args.SAVER_ARG}=${Uri.encode(savers)}" +
                                    "&${args.MISSION_ID_ARG}=${bucket.missionId}",
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
        // 캘린더에서 고른 서버 일정의 상세. 내용은 일정 상세 API 로 채운다
        composable(
            SERVER_SCHEDULE_DETAIL_PATTERN,
            arguments = listOf(
                navArgument(ServerScheduleDetailViewModel.SCHEDULE_ID_ARG) { type = NavType.LongType },
                navArgument(ServerScheduleDetailViewModel.DATE_ARG) { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("fromHome") { type = NavType.BoolType; defaultValue = false },
                navArgument("respond") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            val id = entry.arguments?.getLong(ServerScheduleDetailViewModel.SCHEDULE_ID_ARG)
            val detailViewModel: ServerScheduleDetailViewModel = hiltViewModel(entry)
            // 변경 화면에서 저장하고 돌아오면 바뀐 일정을 다시 불러온다
            val edited by entry.savedStateHandle.getStateFlow(SCHEDULE_EDITED_KEY, false).collectAsStateWithLifecycle()
            LaunchedEffect(edited) {
                if (edited) {
                    entry.savedStateHandle[SCHEDULE_EDITED_KEY] = false
                    detailViewModel.edited()
                }
            }
            ServerScheduleDetailRoute(
                detailViewModel,
                onNearbyMission = onNearbyMission,
                fromHome = entry.arguments?.getBoolean("fromHome") == true,
                respond = entry.arguments?.getBoolean("respond") == true,
                onBack = { navController.popBackStack() },
                onCustom = { navController.navigate("$SERVER_SCHEDULE_ROUTE/$id/custom") { launchSingleTop = true } },
                onRecord = { navController.navigate("$SERVER_SCHEDULE_ROUTE/$id/review") { launchSingleTop = true } },
                onPhotoChange = { navController.navigate("$SERVER_SCHEDULE_ROUTE/$id/photos") { launchSingleTop = true } },
                // 상세 위에 변경 화면을 띄워, 뒤로가기나 저장 후에는 상세로 돌아온다
                onEdit = { scheduleId ->
                    navController.navigate("$SCHEDULE_EDIT_ROUTE/$scheduleId") { launchSingleTop = true }
                },
            )
        }
        // 완료 일정의 대표 사진 변경. 후보는 가족 리뷰 사진이고, 저장하면 대표 사진 변경 API 로 가족 모두에게 반영한다
        dialog("$SERVER_SCHEDULE_ROUTE/{${ServerScheduleDetailViewModel.SCHEDULE_ID_ARG}}/photos", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { entry ->
            val parent = remember(entry) { navController.getBackStackEntry(SERVER_SCHEDULE_DETAIL_PATTERN) }
            val viewModel: ServerScheduleDetailViewModel = hiltViewModel(parent)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            RecordPhotoSelectionContent(
                photos = state.reviewPhotos,
                currentPhotoId = state.representativePhoto?.id,
                selectedPhotoId = state.selectedPhotoId,
                saving = state.detail.saving,
                error = state.detail.error,
                onBack = { navController.popBackStack() },
                onSelect = viewModel::selectRepresentativePhoto,
                onSave = { viewModel.saveRepresentativePhoto { navController.popBackStack() } },
            )
        }
        composable("$SERVER_SCHEDULE_ROUTE/{${ServerScheduleDetailViewModel.SCHEDULE_ID_ARG}}/review") { entry ->
            val parent = remember(entry) { navController.getBackStackEntry(SERVER_SCHEDULE_DETAIL_PATTERN) }
            val viewModel: ServerScheduleDetailViewModel = hiltViewModel(parent)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val context = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.runtime.LaunchedEffect(state.detail.error) {
                val error = state.detail.error ?: return@LaunchedEffect
                android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
            }
            MemoryEditorScreen(state.detail.memoryDraft, state.author,
                onChange = viewModel::updateDraft,
                onBack = { navController.popBackStack() },
                onSubmit = { viewModel.submitReview { navController.popBackStack() } },
                editing = state.editingReviewId != null,
                saving = state.detail.saving, error = state.detail.error)
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
                onCancelled = {
                    navController.popBackStack(SERVER_SCHEDULE_DETAIL_PATTERN, inclusive = true)
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
                "&${BucketScheduleCreateViewModel.SAVER_ARG}={${BucketScheduleCreateViewModel.SAVER_ARG}}" +
                "&${BucketScheduleCreateViewModel.MISSION_ID_ARG}={${BucketScheduleCreateViewModel.MISSION_ID_ARG}}",
            arguments = listOf(
                navArgument(BucketScheduleCreateViewModel.BUCKET_ITEM_ID_ARG) { type = NavType.LongType },
                navArgument(BucketScheduleCreateViewModel.TITLE_ARG) { type = NavType.StringType; defaultValue = "" },
                navArgument(BucketScheduleCreateViewModel.SAVER_ARG) { type = NavType.StringType; defaultValue = "" },
                // 일정 등록 화면의 행사 설명을 미션 상세 API 로 불러온다. 없으면 -1
                navArgument(BucketScheduleCreateViewModel.MISSION_ID_ARG) { type = NavType.LongType; defaultValue = -1L },
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
                onMissionDetail = { id -> onNearbyMission?.invoke(id) },
            )
        }
    }
}
