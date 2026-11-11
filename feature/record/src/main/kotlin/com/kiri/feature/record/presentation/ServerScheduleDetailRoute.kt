package com.kiri.feature.record.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
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

@Composable
fun ServerScheduleDetailRoute(
    viewModel: ServerScheduleDetailViewModel,
    onBack: () -> Unit,
    onCustom: () -> Unit,
    onEdit: (scheduleId: Long) -> Unit = {},
    onRecord: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ServerScheduleDetailEffects(viewModel, onBack, onCustom, onEdit, onRecord)
    var confirmCancel by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { ServerScheduleDetailScreen(state, viewModel::onEvent, viewModel::retryReviews, viewModel::editClicked, viewModel::editReview, viewModel::deleteReview) }
        state.myResponse?.let { Text("내 응답: $it", Modifier.padding(horizontal = 12.dp), color = KiriColor.Text.Secondary) }
        state.detail.error?.let { Text(it, Modifier.padding(12.dp), color = KiriColor.Status.Error) }
        state.notice?.let { Text(it, Modifier.padding(horizontal = 12.dp), color = KiriColor.Text.Accent) }
        ServerScheduleActions(state, viewModel::respond, { confirmCancel = true }, viewModel::openRecord)
    }
    if (confirmCancel) AlertDialog(onDismissRequest = { confirmCancel = false }, title = { Text("일정을 취소할까요?") },
        confirmButton = { TextButton({ confirmCancel = false; viewModel.cancel() }) { Text("취소하기") } },
        dismissButton = { TextButton({ confirmCancel = false }) { Text("돌아가기") } })
}

@Composable
fun ServerScheduleActions(state: ServerScheduleDetailUiState, onRespond: (Boolean) -> Unit, onCancel: () -> Unit, onRecord: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.canRespond) {
            Button({ onRespond(false) }, enabled = !state.detail.saving, modifier = Modifier.testTag("server_schedule_decline")) { Text("거절") }
            Button({ onRespond(true) }, enabled = !state.detail.saving, modifier = Modifier.testTag("server_schedule_accept")) { Text("수락") }
        }
        if (state.canReview && !state.showsRecord && !state.reviewSaved && state.reviews.none { it.isMine }) Button(onRecord, enabled = !state.detail.saving, modifier = Modifier.testTag("server_schedule_review")) { Text("기록 남기기") }
        if (state.canCancel) TextButton(onCancel, enabled = !state.detail.saving, modifier = Modifier.testTag("server_schedule_cancel")) { Text("일정 취소", color = KiriColor.Status.Error) }
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
) {
    val back by rememberUpdatedState(onBack)
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
) {
    val schedule = state.schedule
    if (schedule != null && state.showsRecord) {
        RecordMemoryContent(
            familyName = state.familyName,
            title = state.detail.detail?.title ?: schedule.displayTitle(),
            date = schedule.date,
            participants = state.detail.detail?.participants.orEmpty(),
            representativePhoto = DummyRepresentativePhoto,
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
                CalendarDetailHeader(
                    title = state.appBarTitle,
                    badge = it.status.badgeLabel(),
                    showCustom = state.canCustomize,
                    neutralBadge = it.status.usesNeutralBadge(),
                    collapseParticipants = scheduled || state.canEdit,
                    // 당일이 아닌(예정·확정) 내 일정이면 활동 정보 아래에 일정 변경하기를 보여준다
                    onEditSchedule = onEditClick.takeIf { state.canEdit },
                )
            },
        suggestionIcon = {
            Image(painterResource(R.drawable.record_calendar_umbrella), null, Modifier.size(24.dp))
        },
        nearbyImageRes = R.drawable.record_calendar_nearby,
        placeholderTitle = state.appBarTitle,
        showRetry = state.retryable,
    )
}

// TODO: 서버 일정의 대표 사진 API 가 생기면 바꾼다. 그때까지는 예시 단체 사진을 보여준다
private val DummyRepresentativePhoto = RecordPhoto("dummy-representative", "demo:group")
