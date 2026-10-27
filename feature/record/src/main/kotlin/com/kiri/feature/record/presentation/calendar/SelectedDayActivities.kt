package com.kiri.feature.record.presentation.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.kiri.feature.record.model.*
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography

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
        if (state.selectedEntries.isEmpty() && state.questions[state.selectedDate] == null)
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
