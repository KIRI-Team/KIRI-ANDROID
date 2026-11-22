package com.kiri.feature.record.presentation.schedule

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.domain.record.ScheduleRequestChannel
import com.kiri.feature.record.BucketScheduleCreateEffect
import com.kiri.feature.record.BucketScheduleCreateUiState
import com.kiri.feature.record.BucketScheduleCreateViewModel

// 서버 버킷의 일정 잡기. 화면 흐름은 기존 일정 등록과 같다 (일정 등록하기 → 초대장 미리보기 → 발송 방법)
@Composable
fun BucketScheduleCreateRoute(
    viewModel: BucketScheduleCreateViewModel,
    onBackClick: () -> Unit,
    onSentByApp: () -> Unit,
    onSentByKakao: () -> Unit,
    // 요청 미리보기의 자세히 보러가기. 미션 상세를 연다
    onMissionDetail: (Long) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val sentByApp by rememberUpdatedState(onSentByApp)
    val sentByKakao by rememberUpdatedState(onSentByKakao)
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    BucketScheduleCreateEffect.SentByApp -> sentByApp()
                    is BucketScheduleCreateEffect.SentByKakao -> {
                        // 카카오톡 초대장을 보낸다. 보내지 못해도 일정은 만들어졌으니 화면은 닫는다
                        shareScheduleInviteByKakao(context, effect.scheduleId) { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                        sentByKakao()
                    }
                }
            }
        }
    }
    BucketScheduleCreateScreen(
        state, viewModel::onDraftChange, onBackClick, viewModel::send, viewModel::retry, viewModel::onMonthShown,
        onDetailClick = { state.missionId?.let(onMissionDetail) },
    )
}

@Composable
fun BucketScheduleCreateScreen(
    state: BucketScheduleCreateUiState,
    onDraftChange: (ScheduleRegisterDraft) -> Unit,
    onBackClick: () -> Unit,
    onSend: (ScheduleRequestChannel) -> Unit,
    onRetryClick: () -> Unit,
    onMonthShown: (java.time.YearMonth) -> Unit = {},
    // 요청 미리보기의 자세히 보러가기
    onDetailClick: () -> Unit = {},
) {
    if (state.loading || state.loadError != null) {
        ScheduleFormStatus("일정 등록하기", state.loadError, onBackClick, onRetryClick, "bucket_schedule_status")
        return
    }
    ScheduleRegisterFlow(
        savedBy = state.savedBy,
        title = state.title,
        description = state.description,
        members = state.members,
        draft = state.draft,
        onDraftChange = onDraftChange,
        onBackClick = onBackClick,
        onDetailClick = onDetailClick,
        onSend = onSend,
        sending = state.sending,
        error = state.error,
        earliest = state.earliest,
        blockedDates = state.occupiedDates,
        onMonthShown = onMonthShown,
    )
}
