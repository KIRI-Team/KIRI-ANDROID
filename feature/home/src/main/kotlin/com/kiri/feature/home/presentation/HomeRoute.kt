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

@Composable
fun HomeRoute(
    onDestination: (HomeUiDestination) -> Unit = {},
    onInviteFamily: (String) -> Unit = {},
    onMissionClick: (String) -> Unit = {},
    onScheduleClick: (String) -> Unit = { onDestination(HomeUiDestination.Schedule) },
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentDestination by rememberUpdatedState(onDestination)
    val currentInvite by rememberUpdatedState(onInviteFamily)
    val currentMission by rememberUpdatedState(onMissionClick)
    val currentSchedule by rememberUpdatedState(onScheduleClick)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshOnResume()
            viewModel.effect.collect { effect ->
                when (effect) {
                    is HomeUiEffect.Navigate -> currentDestination(effect.destination)
                    is HomeUiEffect.InviteFamily -> currentInvite(effect.code)
                    is HomeUiEffect.OpenMission -> currentMission(effect.id)
                    is HomeUiEffect.OpenSchedule -> currentSchedule(effect.id)
                }
            }
        }
    }
    HomeScreen(state, viewModel::onEvent)
}
