package com.kiri.feature.record.presentation

import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kiri.feature.record.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.domain.record.CalendarEntryKind
import com.kiri.feature.detail.presentation.CalendarDetailHeader
import com.kiri.feature.detail.presentation.MissionDetailScreen
import com.kiri.feature.record.RecordDetailEffect
import com.kiri.feature.record.RecordDetailViewModel
import java.time.LocalDate
import java.time.ZoneId
import com.kiri.uikit.component.*

@Composable
fun RecordDetailRoute(viewModel: RecordDetailViewModel, onBack: () -> Unit, onCustom: () -> Unit, onPhoto: () -> Unit, onEdit: () -> Unit, onCreateSchedule: (String) -> Unit = {}, onManageSchedule: (String) -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecordDetailEffects(viewModel, onBack, onCustom, onEdit)
    if (state.entry?.kind == CalendarEntryKind.MEMORY && state.detail.detail != null) {
        RecordMemoryScreen(state, onBack, {
            viewModel.beginPhotoSelection()
            onPhoto()
        }, viewModel::editMemory, viewModel::deleteMemory, viewModel::dismissToast)
    } else {
        val date = state.entry?.date
        val bucket = state.entry?.kind == CalendarEntryKind.BUCKET
        // TODO: 오늘 기준 통일 - 실제 RecordRepository 를 만들 때 기기 시계(LocalDate.now)와 저장소의 가족 날짜(family.date) 중 하나로 맞춘다
        val today = date == LocalDate.now(ZoneId.of("Asia/Seoul"))
        var confirmDelete by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxSize()) {
        MissionDetailScreen(state.detail, viewModel::onDetailEvent, modifier = Modifier.weight(1f), calendarHeader = CalendarDetailHeader(
            title = if (bucket) "버킷리스트 저장" else date?.let { "${it.monthValue}월 ${it.dayOfMonth}일" } ?: "기록 상세",
            badge = if (bucket) "버킷리스트로 저장된 약속이에요!" else if (today) "약속 확정" else "끼리추천",
            showCustom = !bucket && today,
        ), suggestionIcon = { Image(painterResource(R.drawable.record_calendar_umbrella), null, Modifier.size(24.dp)) }, nearbyImageRes = R.drawable.record_calendar_nearby)
        state.entry?.let { entry ->
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (bucket) {
                    KiriButton({ if (entry.linkedScheduleIds.isEmpty()) onCreateSchedule(entry.id) else onManageSchedule(entry.linkedScheduleIds.first()) },
                        enabled = !entry.completed && !state.detail.saving, text = if (entry.completed) "완료한 활동" else if (entry.linkedScheduleIds.isEmpty()) "일정 잡기" else "연결된 약속 보기", layout = KiriButtonLayout.Record, testTag = "bucket_schedule")
                    if (entry.saverId == state.collection.currentUserId) TextButton({ confirmDelete = true }, enabled = !state.detail.saving && !entry.completed && entry.linkedScheduleIds.isEmpty()) { Text("버킷 삭제") }
                } else if (entry.kind == CalendarEntryKind.SCHEDULE) KiriButton({ onManageSchedule(entry.id) }, text = "약속 응답·관리", layout = KiriButtonLayout.Record)
            }
        }
        }
        if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("버킷을 삭제할까요?") }, confirmButton = { TextButton({ confirmDelete = false; viewModel.deleteBucket() }) { Text("삭제하기") } }, dismissButton = { TextButton({ confirmDelete = false }) { Text("취소") } })
    }
}

/** Shared entry VM effects are consumed only by the resumed screen (including the photo dialog). */
@Composable
fun RecordDetailEffects(viewModel: RecordDetailViewModel, onBack: () -> Unit, onCustom: () -> Unit = {}, onEdit: () -> Unit = {}, returnToDetail: Boolean = false) {
    val lifecycle = LocalLifecycleOwner.current
    val back by rememberUpdatedState(onBack)
    val custom by rememberUpdatedState(onCustom)
    val edit by rememberUpdatedState(onEdit)
    val canReturn by rememberUpdatedState(returnToDetail)
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.effect.collect {
                when (it) {
                    RecordDetailEffect.Back -> back()
                    RecordDetailEffect.Custom -> custom()
                    RecordDetailEffect.EditMemory -> edit()
                    RecordDetailEffect.ReturnToDetail -> if (canReturn) back()
                }
            }
        }
    }
}
