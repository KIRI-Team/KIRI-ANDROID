package com.kiri.feature.record.presentation.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kiri.domain.model.schedule.CalendarScheduleStatus
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.feature.record.model.*
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography

// 고른 날에 가족 모두 답한 질문이 있으면 읽기 전용으로 보여준다
// 그날의 활동·일정 목록은 보여주지 않는다. 날짜를 누르면 그중 하나의 상세 화면으로 바로 간다
internal fun androidx.compose.foundation.lazy.LazyListScope.selectedDayQuestion(state: RecordUiState) {
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
