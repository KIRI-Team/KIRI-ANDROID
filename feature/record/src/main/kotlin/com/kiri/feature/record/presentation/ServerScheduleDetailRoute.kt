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
import com.kiri.feature.detail.model.DetailUiEvent
import com.kiri.feature.detail.presentation.CalendarDetailHeader
import com.kiri.feature.detail.presentation.MissionDetailScreen
import com.kiri.feature.record.R
import com.kiri.feature.record.ServerScheduleDetailEffect
import com.kiri.feature.record.ServerScheduleDetailUiState
import com.kiri.feature.record.ServerScheduleDetailViewModel
import com.kiri.feature.record.badgeLabel

@Composable
fun ServerScheduleDetailRoute(viewModel: ServerScheduleDetailViewModel, onBack: () -> Unit, onCustom: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ServerScheduleDetailEffects(viewModel, onBack, onCustom)
    ServerScheduleDetailScreen(state, viewModel::onEvent)
}

// 상세 화면과 컨디션 커스텀 화면이 같은 ViewModel 을 쓰므로, 앞에 보이는(RESUMED) 화면만 효과를 받는다
@Composable
fun ServerScheduleDetailEffects(
    viewModel: ServerScheduleDetailViewModel,
    onBack: () -> Unit,
    onCustom: () -> Unit = {},
) {
    val back by rememberUpdatedState(onBack)
    val custom by rememberUpdatedState(onCustom)
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.effect.collect {
                when (it) {
                    ServerScheduleDetailEffect.Back -> back()
                    ServerScheduleDetailEffect.OpenCustom -> custom()
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
                CalendarDetailHeader(
                    title = "${it.date.monthValue}월 ${it.date.dayOfMonth}일",
                    badge = it.status.badgeLabel(),
                    showCustom = state.canCustomize,
                )
            },
        suggestionIcon = {
            Image(painterResource(R.drawable.record_calendar_umbrella), null, Modifier.size(24.dp))
        },
        nearbyImageRes = R.drawable.record_calendar_nearby,
    )
}
