package com.kiri.feature.record.presentation

import androidx.compose.foundation.Image
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
import com.kiri.feature.detail.model.DetailUiEvent
import com.kiri.feature.detail.presentation.CalendarDetailHeader
import com.kiri.feature.detail.presentation.MissionDetailScreen
import com.kiri.feature.record.R
import com.kiri.feature.record.ServerScheduleDetailEffect
import com.kiri.feature.record.ServerScheduleDetailUiState
import com.kiri.feature.record.ServerScheduleDetailViewModel
import com.kiri.feature.record.badgeLabel
import com.kiri.feature.record.usesNeutralBadge

@Composable
fun ServerScheduleDetailRoute(
    viewModel: ServerScheduleDetailViewModel,
    onBack: () -> Unit,
    onCustom: () -> Unit,
    onEdit: (scheduleId: Long) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ServerScheduleDetailEffects(viewModel, onBack, onCustom, onEdit)
    ServerScheduleDetailScreen(state, viewModel::onEvent)
}

// 상세 화면과 컨디션 커스텀 화면이 같은 ViewModel 을 쓰므로, 앞에 보이는(RESUMED) 화면만 효과를 받는다
@Composable
fun ServerScheduleDetailEffects(
    viewModel: ServerScheduleDetailViewModel,
    onBack: () -> Unit,
    onCustom: () -> Unit = {},
    onEdit: (scheduleId: Long) -> Unit = {},
) {
    val back by rememberUpdatedState(onBack)
    val custom by rememberUpdatedState(onCustom)
    val edit by rememberUpdatedState(onEdit)
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.effect.collect {
                when (it) {
                    ServerScheduleDetailEffect.Back -> back()
                    ServerScheduleDetailEffect.OpenCustom -> custom()
                    is ServerScheduleDetailEffect.OpenEdit -> edit(it.scheduleId)
                }
            }
        }
    }
}

// 기록 캘린더의 일정 상세와 같은 화면 형식을 쓴다
@Composable
fun ServerScheduleDetailScreen(state: ServerScheduleDetailUiState, onEvent: (DetailUiEvent) -> Unit) {
    val schedule = state.schedule
    MissionDetailScreen(
        state.detail,
        onEvent,
        calendarHeader =
            schedule?.let {
                // 약속 예정이면 "인원확인" 말풍선으로 참여자를 보여준다
                val scheduled = it.status == ScheduleDisplayStatus.SCHEDULED
                CalendarDetailHeader(
                    title = state.appBarTitle,
                    badge = it.status.badgeLabel(),
                    showCustom = state.canCustomize,
                    neutralBadge = it.status.usesNeutralBadge(),
                    collapseParticipants = scheduled,
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
