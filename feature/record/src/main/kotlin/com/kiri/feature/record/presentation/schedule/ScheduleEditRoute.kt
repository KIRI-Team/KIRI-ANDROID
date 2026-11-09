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
import androidx.compose.runtime.*
import androidx.compose.material3.*
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
    var confirmCancel by remember { mutableStateOf(false) }
    Column {
        Box(Modifier.weight(1f)) { ScheduleEditScreen(state, viewModel::onDraftChange, onBackClick, viewModel::save, viewModel::retry, viewModel::onMonthShown) }
        if (!state.loading && state.loadError == null) TextButton({ confirmCancel = true }, enabled = !state.saving, modifier = Modifier.testTag("server_schedule_cancel")) { Text("일정 취소", color = KiriColor.Status.Error) }
    }
    if (confirmCancel) AlertDialog(onDismissRequest = { confirmCancel = false }, title = { Text("일정을 취소할까요?") },
        confirmButton = { TextButton({ confirmCancel = false; viewModel.cancel() }) { Text("취소하기") } },
        dismissButton = { TextButton({ confirmCancel = false }) { Text("돌아가기") } })
}

@Composable
fun ScheduleEditScreen(
    state: ScheduleEditUiState,
    onDraftChange: (ScheduleRegisterDraft) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onRetryClick: () -> Unit,
    onMonthShown: (java.time.YearMonth) -> Unit = {},
) {
    if (state.loading || state.loadError != null) {
        ScheduleFormStatus("일정 변경하기", state.loadError, onBackClick, onRetryClick, "schedule_edit_status")
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
        blockedDates = state.occupiedDates,
        onMonthShown = onMonthShown,
    )
}

// 일정 등록·변경 화면이 필요한 정보를 불러오는 중이거나 불러오지 못했을 때 보여준다. error 가 null 이면 불러오는 중이다
@Composable
internal fun ScheduleFormStatus(
    appBarTitle: String,
    error: String?,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    testTag: String,
) {
    Column(
        Modifier.fillMaxSize()
            .background(KiriColor.Bg.Base)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag(testTag),
    ) {
        KiriTopBar(onBackClick = onBackClick, title = appBarTitle)
        Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            if (error == null) CircularProgressIndicator()
            else
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(error, color = KiriColor.Text.Primary, style = KiriDetailTypography.Body)
                    KiriDetailButton("다시 시도", onRetryClick)
                }
        }
    }
}
