package com.kiri.feature.record.presentation.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.feature.record.ScheduleEditEffect
import com.kiri.feature.record.ScheduleEditUiState
import com.kiri.feature.record.ScheduleEditViewModel
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailButton
import com.kiri.uikit.component.KiriDetailTypography
import com.kiri.uikit.component.KiriTopBar

// 서버 일정 변경하기. 저장되면 이전 화면(캘린더)으로 돌아간다
@Composable
fun ScheduleEditRoute(viewModel: ScheduleEditViewModel, onBackClick: () -> Unit, onSaved: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saved by rememberUpdatedState(onSaved)
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect {
                when (it) {
                    ScheduleEditEffect.Saved -> saved()
                }
            }
        }
    }
    ScheduleEditScreen(state, viewModel::onDraftChange, onBackClick, viewModel::save, viewModel::retry)
}

@Composable
fun ScheduleEditScreen(
    state: ScheduleEditUiState,
    onDraftChange: (ScheduleRegisterDraft) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    if (state.loading || state.loadError != null) {
        Column(
            Modifier.fillMaxSize()
                .background(KiriColor.Bg.Base)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("schedule_edit_status"),
        ) {
            KiriTopBar(onBackClick = onBackClick, title = "일정 변경하기")
            Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator()
                else
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(state.loadError.orEmpty(), color = KiriColor.Text.Primary, style = KiriDetailTypography.Body)
                        KiriDetailButton("다시 시도", onRetryClick)
                    }
            }
        }
        return
    }
    ScheduleRegisterScreen(
        savedBy = "",
        title = state.title,
        description = "",
        members = state.members,
        times = DefaultScheduleTimes,
        draft = state.draft,
        onDraftChange = onDraftChange,
        onBackClick = onBackClick,
        onCreateInviteClick = onSaveClick,
        earliest = state.earliest,
        mode = ScheduleFormMode.edit(state.subtitle, state.saving, state.error),
    )
}
