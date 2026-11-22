package com.kiri.feature.home.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.domain.home.*
import com.kiri.feature.home.HomeViewModel
import com.kiri.feature.home.model.*
import com.kiri.uikit.permission.NotificationPermissionMoment
import com.kiri.uikit.permission.NotificationPermissionPrompt

@Composable
fun HomeRoute(
    onDestination: (HomeUiDestination) -> Unit = {},
    onInviteFamily: (String) -> Unit = {},
    onMissionClick: (String) -> Unit = {},
    onScheduleClick: (String) -> Unit = { onDestination(HomeUiDestination.Schedule) },
    // 응답이 필요한 일정 카드. 따로 정하지 않으면 일정 상세로 연다
    onRespondSchedule: (String) -> Unit = onScheduleClick,
    viewModel: HomeViewModel = hiltViewModel(),
    onAnswerEditingChanged: (Boolean) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentDestination by rememberUpdatedState(onDestination)
    val currentInvite by rememberUpdatedState(onInviteFamily)
    val currentMission by rememberUpdatedState(onMissionClick)
    val currentSchedule by rememberUpdatedState(onScheduleClick)
    val currentRespond by rememberUpdatedState(onRespondSchedule)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshOnResume()
            viewModel.effect.collect { effect ->
                when (effect) {
                    is HomeUiEffect.Navigate -> currentDestination(effect.destination)
                    is HomeUiEffect.InviteFamily -> currentInvite(effect.code)
                    is HomeUiEffect.OpenMission -> currentMission(effect.id)
                    is HomeUiEffect.OpenSchedule -> if (effect.respond) currentRespond(effect.id) else currentSchedule(effect.id)
                }
            }
        }
    }
    // 약관 화면에서 알림 권한을 거절했거나 묻지 못했으면 홈에서 한 번 더 묻는다
    NotificationPermissionPrompt(NotificationPermissionMoment.HOME)
    val reportEditing by rememberUpdatedState(onAnswerEditingChanged)
    DisposableEffect(state.editing) {
        reportEditing(state.editing)
        onDispose { reportEditing(false) }
    }
    HomeScreen(state, viewModel::onEvent)
}
