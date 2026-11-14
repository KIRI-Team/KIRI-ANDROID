package com.kiri.feature.record.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.kiri.uikit.color.KiriColor
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.domain.record.RecordPhoto
import com.kiri.feature.detail.model.DetailUiEvent
import com.kiri.feature.detail.presentation.CalendarDetailHeader
import com.kiri.feature.detail.presentation.MissionDetailScreen
import com.kiri.feature.record.R
import com.kiri.feature.record.ServerScheduleDetailEffect
import com.kiri.feature.record.ServerScheduleDetailUiState
import com.kiri.feature.record.ServerScheduleDetailViewModel
import com.kiri.feature.record.badgeLabel
import com.kiri.feature.record.displayTitle
import com.kiri.feature.record.label
import com.kiri.feature.record.usesNeutralBadge
import com.kiri.feature.record.presentation.schedule.ScheduleRequestScreen
import com.kiri.feature.record.presentation.schedule.receivedPromiseLabel
import com.kiri.feature.record.presentation.schedule.scheduledPromiseLabel
import androidx.compose.runtime.saveable.rememberSaveable
import com.kiri.uikit.color.KiriComponentColor

@Composable
fun ServerScheduleDetailRoute(
    viewModel: ServerScheduleDetailViewModel,
    onBack: () -> Unit,
    onCustom: () -> Unit,
    onEdit: (scheduleId: Long) -> Unit = {},
    onRecord: () -> Unit = {},
    onPhotoChange: () -> Unit = {},
    fromHome: Boolean = false,
    // 홈의 응답 필요 카드나 기록 탭에서 들어왔다. 응답할 수 있으면 상세 대신 응답 화면을 먼저 보여준다
    respond: Boolean = false,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ServerScheduleDetailEffects(viewModel, onBack, onCustom, onEdit, onRecord, onPhotoChange)
    var confirmCancel by remember { mutableStateOf(false) }
    // 응답 화면의 자세히 보러가기를 누르면 상세로 넘어간다
    var showResponse by rememberSaveable { mutableStateOf(respond) }
    val invitation = state.schedule
    if (showResponse && invitation != null && state.canRespond) {
        // 받은 초대에 응답하는 화면. 요청알림 화면을 그대로 쓰고 아래 버튼만 거절·수락으로 바꾼다
        ScheduleRequestScreen(
            savedBy = "",
            title = state.detail.detail?.title ?: invitation.displayTitle(),
            description = "",
            date = invitation.date,
            memberNames = invitation.participants.map { it.nickname },
            message = "",
            onBackClick = { if (!state.detail.saving) onBack() },
            onDetailClick = { if (!state.detail.saving) { showResponse = false; viewModel.messageShown() } },
            onSendClick = { viewModel.respond(true, leave = true) },
            sending = state.detail.saving,
            error = state.detail.error,
            headline = invitation.participants.firstOrNull()?.nickname?.let(::receivedPromiseLabel),
            dismissText = "거절",
            confirmText = "수락",
            confirmingText = "응답하는 중…",
            dismissBackground = KiriComponentColor.Button.FilledNeutral.BgDefault,
            onDismissClick = { viewModel.respond(false, leave = true) },
        )
        return
    }
    // 응답 화면에서 자세히 보러가기로 왔으면, 상세의 뒤로가기는 응답 화면으로 돌아간다
    val backToResponse = respond && !showResponse && state.canRespond
    androidx.activity.compose.BackHandler(enabled = backToResponse && !confirmCancel) { showResponse = true }
    val onDetailEvent: (DetailUiEvent) -> Unit = { event ->
        if (event == DetailUiEvent.BackClicked && backToResponse) showResponse = true else viewModel.onEvent(event)
    }
    val cancelling = state.schedule
    if (confirmCancel && cancelling != null) {
        // 일정 취소 확인 (Figma 10629:27298). 요청알림 화면을 문구만 바꿔 쓴다. 자세히 보러가기는 상세로 돌아간다
        ScheduleRequestScreen(
            savedBy = "",
            title = state.detail.detail?.title ?: cancelling.displayTitle(),
            description = "",
            date = cancelling.date,
            memberNames = cancelling.participants.map { it.nickname },
            message = "",
            // 취소 화면은 오류를 화면 안에 보여주므로, 닫을 때 지워 상세에서 다시 토스트로 띄우지 않는다
            onBackClick = { if (!state.detail.saving) { confirmCancel = false; viewModel.messageShown() } },
            onDetailClick = { if (!state.detail.saving) { confirmCancel = false; viewModel.messageShown() } },
            onSendClick = viewModel::cancel,
            sending = state.detail.saving,
            error = state.detail.error,
            topBarTitle = "일정 취소",
            headline = scheduledPromiseLabel(cancelling.date),
            dismissText = "돌아가기",
            confirmText = "일정 취소하기",
            confirmingText = "취소하는 중…",
            dismissBackground = KiriComponentColor.Button.FilledNeutral.BgDefault,
        )
        return
    }
    // 작업 결과 안내("수락했어요." 등)와 작업 실패 오류는 토스트로 띄운다.
    // 상세를 불러오지 못한 오류는 화면 안에 다시 시도와 함께 보여주므로 토스트로 띄우지 않는다
    val context = LocalContext.current
    val actionError = state.detail.error?.takeIf { state.detail.detail != null }
    LaunchedEffect(state.notice, actionError) {
        val message = state.notice ?: actionError ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.messageShown()
    }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { ServerScheduleDetailScreen(state, onDetailEvent, viewModel::retryReviews, viewModel::editClicked, viewModel::editReview, viewModel::deleteReview, viewModel::openPhotoChange, fromHome = fromHome, onCancelClick = { confirmCancel = true }) }
        ServerScheduleActions(
            state, { viewModel.respond(it) },
            // 기록 탭(캘린더·다가오는 약속)에서 연 상세에서는 초대에 응답할 수 없다. 응답은 홈의 응답 필요 카드에서 한다.
            // 응답 화면에서 자세히 보러가기로 온 상세도 뒤로가기로 응답 화면에 돌아가 응답하므로 숨긴다
            showRespond = fromHome && !respond,
        )
    }
}

// 일정 변경하기를 보여줄지. 당일이 아닌(예정·확정) 내 일정이면 어디서 들어왔든 보여준다.
// 상세 API 는 응답 대기와 미확정(아무도 수락하지 않고 마감)을 모두 예정(SCHEDULED)으로 주므로 둘을 구분하지 않는다
internal val ServerScheduleDetailUiState.showsEditButton: Boolean
    get() = canEdit

// 일정 취소하기를 보여줄지. 내가 제안한 취소 전 일정이면 어디서 들어왔든(확정 전이어도) 보여준다
internal val ServerScheduleDetailUiState.showsCancelButton: Boolean
    get() = canEdit || canCancel

@Composable
fun ServerScheduleActions(state: ServerScheduleDetailUiState, onRespond: (Boolean) -> Unit, showRespond: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showRespond && state.canRespond) {
            Button({ onRespond(false) }, enabled = !state.detail.saving, modifier = Modifier.testTag("server_schedule_decline")) { Text("거절") }
            Button({ onRespond(true) }, enabled = !state.detail.saving, modifier = Modifier.testTag("server_schedule_accept")) { Text("수락") }
        }
    }
}

// 상세 화면과 컨디션 커스텀 화면이 같은 ViewModel 을 쓰므로, 앞에 보이는(RESUMED) 화면만 효과를 받는다
@Composable
fun ServerScheduleDetailEffects(
    viewModel: ServerScheduleDetailViewModel,
    onBack: () -> Unit,
    onCustom: () -> Unit = {},
    onEdit: (scheduleId: Long) -> Unit = {},
    onRecord: () -> Unit = {},
    onPhotoChange: () -> Unit = {},
) {
    val back by rememberUpdatedState(onBack)
    val photoChange by rememberUpdatedState(onPhotoChange)
    val custom by rememberUpdatedState(onCustom)
    val edit by rememberUpdatedState(onEdit)
    val record by rememberUpdatedState(onRecord)
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.effect.collect {
                when (it) {
                    ServerScheduleDetailEffect.Back -> back()
                    ServerScheduleDetailEffect.OpenCustom -> custom()
                    ServerScheduleDetailEffect.OpenRecord -> record()
                    is ServerScheduleDetailEffect.OpenEdit -> edit(it.scheduleId)
                    ServerScheduleDetailEffect.OpenPhotoChange -> photoChange()
                }
            }
        }
    }
}

// 기록 캘린더의 일정 상세와 같은 화면 형식을 쓴다. 완료된 일정은 기록 화면(Figma 7903:1926)으로 보여준다
@Composable
fun ServerScheduleDetailScreen(
    state: ServerScheduleDetailUiState,
    onEvent: (DetailUiEvent) -> Unit,
    onRetryReviews: () -> Unit = {},
    onEditClick: () -> Unit = {},
    // 내 리뷰 카드의 수정하기
    onEditReview: (reviewId: Long) -> Unit = {},
    // 내 리뷰 카드의 ⋯ > 삭제 (확인 창에서 삭제를 누른 뒤)
    onDeleteReview: (reviewId: Long) -> Unit = {},
    // 대표 사진의 사진 변경
    onChangePhoto: () -> Unit = {},
    fromHome: Boolean = false,
    // 일정 취소하기 (일정 취소 확인 화면을 띄운다)
    onCancelClick: () -> Unit = {},
) {
    val schedule = state.schedule
    if (schedule != null && state.showsRecord && !fromHome) {
        RecordMemoryContent(
            familyName = state.familyName,
            title = state.detail.detail?.title ?: schedule.displayTitle(),
            date = schedule.date,
            participants = state.detail.detail?.participants.orEmpty(),
            // 정한 대표 사진, 없으면 리뷰 목록의 첫 사진. 고를 사진이 두 장 이상이면 사진 변경을 보여준다
            representativePhoto = state.representativePhoto,
            onChangePhoto = onChangePhoto.takeIf { state.reviewPhotos.size > 1 },
            memories = state.reviews.map { review ->
                MemoryCardUi(
                    id = review.id.toString(),
                    authorName = review.writer.nickname,
                    role = review.writer.familyRole.label,
                    text = review.content,
                    photos = review.imageUrls.mapIndexed { index, url -> RecordPhoto("review-${review.id}-$index", url) },
                    mine = review.isMine,
                )
            },
            onBack = { onEvent(DetailUiEvent.BackClicked) },
            // 내 리뷰에만 수정하기와 ⋯(삭제)를 보여준다 (RecordMemoryContent 가 mine 으로 거르고, 삭제는 확인 창을 띄운다)
            onEdit = { id -> id.toLongOrNull()?.let(onEditReview) },
            onDelete = { id -> id.toLongOrNull()?.let(onDeleteReview) },
            saving = state.detail.saving,
            onReview = ({ onEvent(DetailUiEvent.RecordClicked) }).takeIf { state.canReview && !state.reviewSaved && state.reviews.none { it.isMine } },
            memoriesLoading = state.reviewsLoading,
            onRetryMemories = onRetryReviews.takeIf { state.reviewsFailed },
        )
        return
    }
    MissionDetailScreen(
        state.screenDetail,
        onEvent,
        calendarHeader =
            schedule?.let {
                // 약속 예정이거나 제안자 화면이면 "인원확인" 말풍선으로 참여자를 보여준다 (Figma 9898:20930, 9898:21783)
                val scheduled = it.status == ScheduleDisplayStatus.SCHEDULED
                val todayFromHome = fromHome && it.status == ScheduleDisplayStatus.TODAY
                val confirmedFromHome = fromHome && it.status == ScheduleDisplayStatus.CONFIRMED
                CalendarDetailHeader(
                    title = state.appBarTitle,
                    badge = it.status.badgeLabel(),
                    showCustom = state.canCustomize && !todayFromHome,
                    // 오늘의 추억 기록하러가기. 당일이거나 기록을 남길 수 있는 일정(수락한 시작된 일정)이면 홈·기록 탭 모두 보여준다.
                    // 기록 탭의 완료 일정은 위의 기록 화면으로 보여준다
                    showRecord = it.status == ScheduleDisplayStatus.TODAY || state.canReview || (fromHome && state.showsRecord),
                    showConditionsInNearby = todayFromHome,
                    recordEnabled = state.canReview && !state.detail.saving && !state.reviewSaved && state.reviews.none { it.isMine },
                    neutralBadge = it.status.usesNeutralBadge(),
                    collapseParticipants = !todayFromHome && (scheduled || confirmedFromHome || state.canEdit),
                    // 당일이 아닌(예정·확정) 내 일정이면 활동 정보 아래에 일정 변경하기·일정 취소하기를 보여준다
                    onEditSchedule = onEditClick.takeIf { state.showsEditButton },
                    onCancelSchedule = onCancelClick.takeIf { state.showsCancelButton },
                )
            },
        suggestionIcon = {
            Image(painterResource(R.drawable.record_calendar_umbrella), null, Modifier.size(24.dp))
        },
        nearbyImageRes = R.drawable.record_calendar_nearby,
        placeholderTitle = state.appBarTitle,
        showRetry = state.retryable,
        familyMemories = if (fromHome && (state.canCustomize || state.showsRecord)) {
            { HomeScheduleMemories(state, onRetryReviews, onEditReview, onDeleteReview) }
        } else null,
    )
}


@Composable
private fun HomeScheduleMemories(
    state: ServerScheduleDetailUiState,
    onRetry: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var deleteId by remember { mutableStateOf<Long?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when {
            state.reviewsLoading -> CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
            state.reviewsFailed -> {
                Text("기록을 불러오지 못했어요.", color = KiriColor.Text.Secondary)
                com.kiri.uikit.component.KiriDetailButton("다시 시도", onRetry)
            }
            state.reviews.isEmpty() -> com.kiri.uikit.component.KiriEmptyState(
                "아직 기록이 없어요!", Modifier.testTag("family_memories_empty"),
            )
            else -> {
                val family = state.familyName?.takeIf { it.isNotBlank() } ?: "우리 가족"
                Text("$family 님의 한 줄", style = com.kiri.uikit.KiriTypography.TitleLarge, color = KiriColor.Text.Strong)
                state.reviews.forEach { review ->
                    FamilyMemoryCard(
                        MemoryCardUi(
                            id = review.id.toString(),
                            authorName = review.writer.nickname,
                            role = review.writer.familyRole.label,
                            text = review.content,
                            photos = review.imageUrls.mapIndexed { index, url -> RecordPhoto("review-${review.id}-$index", url) },
                            mine = review.isMine,
                        ),
                        saving = state.detail.saving,
                        onEdit = ({ onEdit(review.id) }).takeIf { review.isMine },
                        onDelete = ({ deleteId = review.id }).takeIf { review.isMine },
                    )
                }
            }
        }
    }
    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("기록을 삭제할까요?") },
            text = { Text("삭제한 기록은 다시 복구할 수 없어요.") },
            confirmButton = { TextButton(onClick = { deleteId = null; onDelete(id) }) { Text("삭제하기") } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("취소") } },
        )
    }
}
