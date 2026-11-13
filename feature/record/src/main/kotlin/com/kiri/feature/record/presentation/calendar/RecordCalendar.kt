package com.kiri.feature.record.presentation.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.feature.record.R
import com.kiri.feature.record.model.*
import com.kiri.feature.record.presentation.sheet.recordCalendarWeeks
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography

@Composable
internal fun RecordCalendar(
    state: RecordUiState,
    onEvent: (RecordUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.fillMaxWidth().testTag("record_calendar"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 24.dp),
    ) {
        item(key = "month") {
            Row(
                Modifier.fillMaxWidth().height(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${state.month.year}년 ${state.month.monthValue}월",
                    style = KiriDetailTypography.Title,
                    color = KiriColor.Text.Strong,
                    modifier = Modifier.testTag("record_month"),
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = KiriColor.Primary.Default.copy(alpha = .2f),
                ) {
                    Row(
                        Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "알림",
                            style =
                                KiriDetailTypography.Small.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    letterSpacing = (-.48).sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            color = KiriColor.Text.Accent,
                        )
                        Image(
                            painterResource(R.drawable.record_notice_dot),
                            null,
                            Modifier.size(6.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(64.dp).height(48.dp), contentAlignment = Alignment.Center) {
                    Image(
                        painterResource(R.drawable.record_month_arrows),
                        null,
                        Modifier.size(32.dp, 16.dp),
                    )
                    Row(Modifier.fillMaxSize()) {
                        Box(
                            Modifier.weight(1f)
                                .fillMaxHeight()
                                .clickable(role = Role.Button) {
                                    onEvent(RecordUiEvent.PreviousMonth)
                                }
                                .testTag("record_previous_month")
                                .semantics { contentDescription = "이전 달" }
                        )
                        Box(
                            Modifier.weight(1f)
                                .fillMaxHeight()
                                .clickable(role = Role.Button) { onEvent(RecordUiEvent.NextMonth) }
                                .testTag("record_next_month")
                                .semantics { contentDescription = "다음 달" }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("월", "화", "수", "목", "금", "토", "일").forEach { weekday ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            weekday,
                            color = KiriColor.Text.Tertiary,
                            style =
                                KiriDetailTypography.Small.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-.48).sp,
                                ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        // 날짜 선택 시트와 같은 주 계산을 쓴다. 이 화면은 다음 달 날짜를 그리지 않으므로 빈 칸으로 바꾼다
        recordCalendarWeeks(state.month)
            .map { week -> week.map { day -> day?.takeIf { it.isInMonth }?.date } }
            .forEachIndexed { index, week ->
                item(key = "week-$index") {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        week.forEach { date ->
                            if (date == null) Spacer(Modifier.weight(1f).aspectRatio(1f))
                            else
                                CalendarCell(
                                    date,
                                    state.calendarMarks[date],
                                    date == state.selectedDate,
                                    // 그날 활동 중 하나로 이동할지는 ViewModel 이 정한다
                                    { onEvent(RecordUiEvent.DateSelected(date)) },
                                    Modifier.weight(1f),
                                    state.calendarPhotos[date],
                                    state.entries.count { it.date == date },
                                    today = date == state.today,
                                    past = date < state.today,
                                )
                        }
                    }
                }
            }
        // 날짜를 눌러도 그날 활동 목록은 보여주지 않는다. 가족 모두 답한 질문만 읽기 전용으로 보여준다
        selectedDayQuestion(state)
        item(key = "upcoming-title") {
            Text(
                "다가오는 약속",
                style = KiriDetailTypography.Title,
                color = KiriColor.Text.Strong,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
            )
        }
        if (state.upcomingSchedules.isEmpty())
            item(key = "upcoming-empty") {
                Text(
                    if (state.upcomingFailed) "다가오는 약속을 불러오지 못했어요" else "다가오는 약속이 없어요",
                    style = KiriDetailTypography.Small,
                    color = KiriColor.Text.Secondary,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        items(state.upcomingSchedules, key = { "schedule-${it.id}" }) { schedule ->
            ScheduleCard(schedule, onOpen = { onEvent(RecordUiEvent.ServerScheduleClicked(schedule.id)) })
            Spacer(Modifier.height(18.dp))
        }
    }
}
