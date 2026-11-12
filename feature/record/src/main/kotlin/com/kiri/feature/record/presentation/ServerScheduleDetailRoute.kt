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
    onPhotoChange: () -> Unit = {},
    fromHome: Boolean = false,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ServerScheduleDetailEffects(viewModel, onBack, onCustom, onEdit, onRecord, onPhotoChange)
    var confirmCancel by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { ServerScheduleDetailScreen(state, viewModel::onEvent, viewModel::retryReviews, viewModel::editClicked, viewModel::editReview, viewModel::deleteReview, viewModel::openPhotoChange, fromHome = fromHome) }
        state.myResponse?.let { Text("내 응답: $it", Modifier.padding(horizontal = 12.dp), color = KiriColor.Text.Secondary) }
        state.detail.error?.let { Text(it, Modifier.padding(12.dp), color = KiriColor.Status.Error) }
        state.notice?.let { Text(it, Modifier.padding(horizontal = 12.dp), color = KiriColor.Text.Accent) }
        val confirmedFromHome = fromHome && state.schedule?.status in setOf(
            ScheduleDisplayStatus.CONFIRMED, ScheduleDisplayStatus.TODAY, ScheduleDisplayStatus.COMPLETED,
        )
        ServerScheduleActions(
            state, viewModel::respond, { confirmCancel = true }, viewModel::openRecord,
            showReview = !confirmedFromHome,
            showCancel = !confirmedFromHome,
        )
    }
    if (confirmCancel) AlertDialog(onDismissRequest = { confirmCancel = false }, title = { Text("일정을 취소할까요?") },
        confirmButton = { TextButton({ confirmCancel = false; viewModel.cancel() }) { Text("취소하기") } },
        dismissButton = { TextButton({ confirmCancel = false }) { Text("돌아가기") } })
}

@Composable
fun ServerScheduleActions(state: ServerScheduleDetailUiState, onRespond: (Boolean) -> Unit, onCancel: () -> Unit, onRecord: () -> Unit = {}, showReview: Boolean = true, showCancel: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.canRespond) {
            Button({ onRespond(false) }, enabled = !state.detail.saving, modifier = Modifier.testTag("server_schedule_decline")) { Text("거절") }
            Button({ onRespond(true) }, enabled = !state.detail.saving, modifier = Modifier.testTag("server_schedule_accept")) { Text("수락") }
        }
        if (showReview && state.canReview && !state.showsRecord && !state.reviewSaved && state.reviews.none { it.isMine }) Button(onRecord, enabled = !state.detail.saving, modifier = Modifier.testTag("server_schedule_review")) { Text("기록 남기기") }
        if (showCancel && state.canCancel) TextButton(onCancel, enabled = !state.detail.saving, modifier = Modifier.testTag("server_schedule_cancel")) { Text("일정 취소", color = KiriColor.Status.Error) }
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
                    showRecord = todayFromHome || (fromHome && state.showsRecord),
                    showConditionsInNearby = todayFromHome,
                    recordEnabled = state.canReview && !state.detail.saving && !state.reviewSaved && state.reviews.none { it.isMine },
                    neutralBadge = it.status.usesNeutralBadge(),
                    collapseParticipants = !todayFromHome && (scheduled || confirmedFromHome || state.canEdit),
                    // 당일이 아닌(예정·확정) 내 일정이면 활동 정보 아래에 일정 변경하기를 보여준다
                    onEditSchedule = onEditClick.takeIf { state.canEdit && (!fromHome || confirmedFromHome) },
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
