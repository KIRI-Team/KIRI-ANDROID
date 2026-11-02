package com.kiri.feature.record.presentation.schedule

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.domain.record.ScheduleRequestChannel
import com.kiri.feature.record.ScheduleRegisterEffect
import com.kiri.feature.record.ScheduleRegisterViewModel
import com.kiri.feature.record.presentation.sheet.RecordMember
import com.kiri.feature.record.presentation.sheet.RecordSendMethodSheet
import java.time.LocalDateTime
import java.time.LocalTime

// 고를 수 있는 시간. 서버에서 받기 전까지 09:00 ~ 21:00, 30분 단위로 둔다
internal val DefaultScheduleTimes: List<LocalTime> = (0..24).map { LocalTime.of(9, 0).plusMinutes(30L * it) }

// 버킷 카드에서 들어온 일정 등록. 요청 저장과 화면 이동 신호는 ScheduleRegisterViewModel 이 맡는다
@Composable
fun ScheduleRegisterRoute(
    viewModel: ScheduleRegisterViewModel,
    onBackClick: () -> Unit,
    onDetailClick: () -> Unit,
    onBucketMissing: () -> Unit,
    onSentByApp: () -> Unit,
    onSentByKakao: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBucketMissing by rememberUpdatedState(onBucketMissing)
    val currentOnSentByApp by rememberUpdatedState(onSentByApp)
    val currentOnSentByKakao by rememberUpdatedState(onSentByKakao)
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    ScheduleRegisterEffect.BucketMissing -> currentOnBucketMissing()
                    ScheduleRegisterEffect.SentByApp -> currentOnSentByApp()
                    is ScheduleRegisterEffect.SentByKakao -> {
                        // 카카오톡을 포함해 설치된 앱을 고를 수 있는 시스템 공유 시트를 연다
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, effect.shareText)
                        }
                        context.startActivity(Intent.createChooser(send, null))
                        currentOnSentByKakao()
                    }
                }
            }
        }
    }
    if (!state.bucketFound) return
    ScheduleRegisterFlow(
        savedBy = state.savedBy,
        title = state.title,
        // TODO: 기록 데이터에 활동 설명이 생기면 연결. 비어 있으면 화면에서 설명을 숨긴다
        description = "",
        members = state.members,
        draft = state.draft,
        onDraftChange = viewModel::onDraftChange,
        onBackClick = onBackClick,
        onDetailClick = onDetailClick,
        onSend = viewModel::send,
        sending = state.sending,
        error = state.error,
        earliest = state.earliest,
        weekly = state.weekly,
    )
}

// 일정 등록하기 → (초대장 만들기) → 요청알림 미리보기 → (요청 보내기) → 발송 방법 시트.
// 미리보기에서 취소나 뒤로가기를 누르면 적어 둔 내용 그대로 등록 화면으로 돌아온다
@Composable
internal fun ScheduleRegisterFlow(
    savedBy: String,
    title: String,
    description: String,
    members: List<RecordMember>,
    draft: ScheduleRegisterDraft,
    onDraftChange: (ScheduleRegisterDraft) -> Unit,
    onBackClick: () -> Unit,
    onDetailClick: () -> Unit,
    onSend: (ScheduleRequestChannel) -> Unit,
    sending: Boolean = false,
    error: String? = null,
    earliest: LocalDateTime? = null,
    times: List<LocalTime> = DefaultScheduleTimes,
    weekly: Boolean = false,
) {
    var showRequest by rememberSaveable { mutableStateOf(false) }
    var showSendSheet by rememberSaveable { mutableStateOf(false) }
    val date = draft.date

    if (showRequest && date != null) {
        ScheduleRequestScreen(
            savedBy = savedBy,
            title = title,
            description = description,
            date = date,
            memberNames = members.filter { it.id in draft.memberIds }.map { it.name },
            message = draft.message,
            onBackClick = { if (!sending) showRequest = false },
            onDetailClick = onDetailClick,
            onSendClick = { showSendSheet = true },
            sending = sending,
            error = error,
            weekly = weekly,
        )
        if (showSendSheet) {
            RecordSendMethodSheet(
                onAppClick = {
                    showSendSheet = false
                    onSend(ScheduleRequestChannel.APP)
                },
                onKakaoClick = {
                    showSendSheet = false
                    onSend(ScheduleRequestChannel.KAKAO)
                },
                onDismiss = { showSendSheet = false },
            )
        }
    } else {
        ScheduleRegisterScreen(
            savedBy = savedBy,
            title = title,
            description = description,
            members = members,
            times = times,
            draft = draft,
            onDraftChange = onDraftChange,
            onBackClick = onBackClick,
            onCreateInviteClick = { showRequest = true },
            earliest = earliest,
            weekly = weekly,
        )
    }
}
