package com.kiri.feature.record.presentation.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.kiri.domain.model.schedule.CalendarScheduleStatus
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.feature.record.model.*
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography
import java.time.format.DateTimeFormatter

internal fun androidx.compose.foundation.lazy.LazyListScope.selectedDayActivities(
    state: RecordUiState,
    onEvent: (RecordUiEvent) -> Unit,
) {
    item(key = "selected-date") {
        Text(
            "${state.selectedDate.monthValue}월 ${state.selectedDate.dayOfMonth}일의 활동",
            style = KiriDetailTypography.Title,
            color = KiriColor.Text.Strong,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )
        if (
            state.selectedEntries.isEmpty() &&
                state.selectedSchedules.isEmpty() &&
                state.questions[state.selectedDate] == null
        )
            Text(
                "이 날의 활동이 없어요",
                style = KiriDetailTypography.Small,
                color = KiriColor.Text.Tertiary,
            )
    }
    items(state.selectedEntries, key = { "day-${it.id}" }) { entry ->
        Surface(
            onClick = { onEvent(RecordUiEvent.CalendarEntryClicked(entry.id)) },
            color = KiriColor.Bg.Primary,
            shape = RoundedCornerShape(12.dp),
            modifier =
                Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("record_day_${entry.id}"),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(entry.title, style = KiriDetailTypography.Title, color = KiriColor.Text.Strong)
                Text(
                    "${entry.startTime ?: "시간 미정"} · ${when(entry.kind) { com.kiri.domain.record.CalendarEntryKind.MEMORY -> "활동 기록"
 com.kiri.domain.record.CalendarEntryKind.SCHEDULE -> "약속"
 else -> "버킷리스트" }}",
                    style = KiriDetailTypography.Small,
                    color = KiriColor.Text.Secondary,
                )
            }
        }
    }
    items(state.selectedSchedules, key = { "server-schedule-${it.id}" }) { schedule ->
        ServerScheduleRow(schedule, onEvent)
    }
    state.questions[state.selectedDate]?.let { question ->
        item(key = "past-question") {
            Column(
                Modifier.fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .testTag("record_question_read_only")
            ) {
                Text(
                    question.text,
                    style = KiriDetailTypography.Title,
                    color = KiriColor.Text.Strong,
                )
                Text(
                    "이 날의 가족 답변 · 읽기 전용",
                    style = KiriDetailTypography.Small,
                    color = KiriColor.Text.Tertiary,
                )
                question.answers.forEach { (id, answer) ->
                    Text(
                        "${question.memberNames[id] ?: "탈퇴한 멤버"} · $answer",
                        style = KiriDetailTypography.Body,
                        color = KiriColor.Text.Secondary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

// 서버 일정 한 건. 누르면 일정 상세 화면으로 간다 (내용은 상세 화면에서 불러온다)
// 취소된 일정은 상세를 볼 수 없어 누를 수 없고 흐리게 보여준다
@Composable
private fun ServerScheduleRow(schedule: CalendarSchedule, onEvent: (RecordUiEvent) -> Unit) {
    val (label, confirmed) = scheduleLabel(schedule.status, null)
    Surface(
        color = KiriColor.Bg.Primary,
        shape = RoundedCornerShape(12.dp),
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = 4.dp)
                .alpha(if (schedule.canOpen) 1f else .4f)
                .testTag("record_server_schedule_${schedule.id}"),
        onClick = { onEvent(RecordUiEvent.ServerScheduleClicked(schedule.id)) },
        enabled = schedule.canOpen,
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color =
                    if (confirmed) KiriColor.Primary.Default.copy(alpha = .2f)
                    else KiriColor.Bg.Elevated,
                shape = RoundedCornerShape(5.dp),
            ) {
                Text(
                    label,
                    style = KiriDetailTypography.Small,
                    color = if (confirmed) KiriColor.Text.Accent else KiriColor.Text.Secondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            Text(
                "${schedule.time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "시간 미정"} · 약속",
                style = KiriDetailTypography.Small,
                color = KiriColor.Text.Secondary,
            )
        }
    }
}

// 캘린더에서는 예정·확정·완료·취소로만 나눈다. 당일(TODAY) 문구는 홈 화면에서 쓰므로 여기서는 확정으로 보여준다
// 상세 상태를 모르면 월간 조회 상태로 정한다. 두 번째 값은 확정 배지 색을 쓸지 여부다
internal fun scheduleLabel(status: CalendarScheduleStatus, detailStatus: ScheduleDisplayStatus?): Pair<String, Boolean> =
    when {
        status == CalendarScheduleStatus.CANCELLED -> "약속 취소" to false
        detailStatus == ScheduleDisplayStatus.COMPLETED -> "약속 완료" to false
        detailStatus == ScheduleDisplayStatus.CONFIRMED || detailStatus == ScheduleDisplayStatus.TODAY ->
            "약속 확정" to true
        detailStatus == ScheduleDisplayStatus.SCHEDULED -> "약속 예정" to false
        status == CalendarScheduleStatus.COMPLETED -> "약속 완료" to false
        status == CalendarScheduleStatus.CONFIRMED -> "약속 확정" to true
        else -> "약속 예정" to false
    }
