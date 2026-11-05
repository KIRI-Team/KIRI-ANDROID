package com.kiri.feature.record.presentation.schedule

import android.content.Intent
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
                        // 카카오톡을 포함해 설치된 앱을 고를 수 있는 시스템 공유 시트를 연다
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, effect.shareText)
                        }
                        context.startActivity(Intent.createChooser(send, null))
                        sentByKakao()
                    }
                }
            }
        }
    }
    BucketScheduleCreateScreen(state, viewModel::onDraftChange, onBackClick, viewModel::send, viewModel::retry)
}

@Composable
fun BucketScheduleCreateScreen(
    state: BucketScheduleCreateUiState,
    onDraftChange: (ScheduleRegisterDraft) -> Unit,
    onBackClick: () -> Unit,
    onSend: (ScheduleRequestChannel) -> Unit,
    onRetryClick: () -> Unit,
) {
    if (state.loading || state.loadError != null) {
        ScheduleFormStatus("일정 등록하기", state.loadError, onBackClick, onRetryClick, "bucket_schedule_status")
        return
    }
    ScheduleRegisterFlow(
        savedBy = state.savedBy,
        title = state.title,
        description = "",
        members = state.members,
        draft = state.draft,
        onDraftChange = onDraftChange,
        onBackClick = onBackClick,
        // 서버 버킷의 활동 상세 화면은 아직 없다
        onDetailClick = {},
        onSend = onSend,
        sending = state.sending,
        error = state.error,
        earliest = state.earliest,
    )
}
