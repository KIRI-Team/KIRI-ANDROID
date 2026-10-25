package com.kiri.feature.record.presentation.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.feature.record.R
import com.kiri.feature.record.model.*
import com.kiri.feature.record.presentation.RecordPhotoImage
import com.kiri.feature.record.presentation.sheet.recordCalendarWeeks
import com.kiri.domain.record.RecordPhoto
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun RecordCalendar(state: RecordUiState, onEvent: (RecordUiEvent) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxWidth().testTag("record_calendar"), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 24.dp)) {
        item(key = "month") {
            Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${state.month.year}년 ${state.month.monthValue}월", style = KiriDetailTypography.Title, color = KiriColor.Text.Strong, modifier = Modifier.testTag("record_month"))
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(30.dp), color = KiriColor.Primary.Default.copy(alpha = .2f)) {
                    Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("알림", style = KiriDetailTypography.Small.copy(fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = (-.48).sp, fontWeight = FontWeight.Medium), color = KiriColor.Text.Accent)
                        Image(painterResource(R.drawable.record_notice_dot), null, Modifier.size(6.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(64.dp).height(48.dp), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.record_month_arrows), null, Modifier.size(32.dp, 16.dp))
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Button) { onEvent(RecordUiEvent.PreviousMonth) }
                            .testTag("record_previous_month").semantics { contentDescription = "이전 달" })
                        Box(Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Button) { onEvent(RecordUiEvent.NextMonth) }
                            .testTag("record_next_month").semantics { contentDescription = "다음 달" })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("월", "화", "수", "목", "금", "토", "일").forEach { weekday ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(weekday, color = KiriColor.Text.Tertiary, style = KiriDetailTypography.Small.copy(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.48).sp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        // 날짜 선택 시트와 같은 주 계산을 쓴다. 이 화면은 다음 달 날짜를 그리지 않으므로 빈 칸으로 바꾼다
        recordCalendarWeeks(state.month).map { week -> week.map { day -> day?.takeIf { it.isInMonth }?.date } }.forEachIndexed { index, week ->
            item(key = "week-$index") {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { date ->
                        if (date == null) Spacer(Modifier.weight(1f).aspectRatio(1f))
                        else CalendarCell(date, state.calendarMarks[date], date == state.selectedDate,
                            {
                                onEvent(RecordUiEvent.DateSelected(date))
                                if (state.questions[date] == null) state.entries.filter { it.date == date }.singleOrNull()?.let { onEvent(RecordUiEvent.CalendarEntryClicked(it.id)) }
                            }, Modifier.weight(1f), state.calendarPhotos[date], state.entries.count { it.date == date })
                    }
                }
            }
        }
        item(key = "selected-date") {
            Text("${state.selectedDate.monthValue}월 ${state.selectedDate.dayOfMonth}일의 활동", style = KiriDetailTypography.Title, color = KiriColor.Text.Strong, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
            if (state.selectedEntries.isEmpty() && state.questions[state.selectedDate] == null) Text("이 날의 활동이 없어요", style = KiriDetailTypography.Small, color = KiriColor.Text.Tertiary)
        }
        items(state.selectedEntries, key = { "day-${it.id}" }) { entry ->
            Surface(onClick = { onEvent(RecordUiEvent.CalendarEntryClicked(entry.id)) }, color = KiriColor.Bg.Primary, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("record_day_${entry.id}")) {
                Column(Modifier.padding(16.dp)) {
                    Text(entry.title, style = KiriDetailTypography.Title, color = KiriColor.Text.Strong)
                    Text("${entry.startTime ?: "시간 미정"} · ${when(entry.kind) { com.kiri.domain.record.CalendarEntryKind.MEMORY -> "활동 기록"; com.kiri.domain.record.CalendarEntryKind.SCHEDULE -> "약속"; else -> "버킷리스트" }}", style = KiriDetailTypography.Small, color = KiriColor.Text.Secondary)
                }
            }
        }
        state.questions[state.selectedDate]?.let { question -> item(key = "past-question") {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag("record_question_read_only")) {
                Text(question.text, style = KiriDetailTypography.Title, color = KiriColor.Text.Strong)
                Text("이 날의 가족 답변 · 읽기 전용", style = KiriDetailTypography.Small, color = KiriColor.Text.Tertiary)
                question.answers.forEach { (id, answer) -> Text("${question.memberNames[id] ?: "탈퇴한 멤버"} · $answer", style = KiriDetailTypography.Body, color = KiriColor.Text.Secondary, modifier = Modifier.padding(top = 8.dp)) }
            }
        } }
        item(key = "upcoming-title") {
            Text("다가오는 약속", style = KiriDetailTypography.Title, color = KiriColor.Text.Strong,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))
        }
        if (state.upcomingSchedules.isEmpty()) item { Text("다가오는 약속이 없어요", style = KiriDetailTypography.Small, color = KiriColor.Text.Secondary, modifier = Modifier.padding(vertical = 24.dp)) }
        items(state.upcomingSchedules, key = { "schedule-${it.id}" }) { schedule ->
            ScheduleCard(schedule, onOpen = { onEvent(RecordUiEvent.ScheduleClicked(schedule.id)) })
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun CalendarCell(date: LocalDate, mark: RecordCalendarMark?, selected: Boolean, onClick: () -> Unit, modifier: Modifier, photo: RecordPhoto? = null, count: Int = 0) {
    val shape = RoundedCornerShape(12.dp)
    val background = when {
        selected -> KiriColor.Primary.Default.copy(alpha = .2f)
        mark == RecordCalendarMark.QUESTION || mark == RecordCalendarMark.PHOTO_FIRST || mark == RecordCalendarMark.PHOTO_SECOND -> KiriColor.Bg.Base
        mark == RecordCalendarMark.CONFIRMED -> KiriColor.Bg.GreenSoft
        mark == RecordCalendarMark.PLANNED || mark == RecordCalendarMark.BUCKET -> KiriColor.Bg.Secondary
        else -> KiriColor.Bg.Primary
    }
    Box(
        modifier.aspectRatio(1f).clip(shape).background(background)
            .then(if (selected) Modifier.border(1.dp, KiriColor.Primary.Default, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick).testTag("record_date_$date")
            .semantics {
                this.selected = selected
                contentDescription = "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일" + when (mark) {
                    RecordCalendarMark.PLANNED -> ", 약속 예정"
                    RecordCalendarMark.CONFIRMED -> ", 약속 확정"
                    RecordCalendarMark.BUCKET -> ", 버킷 활동"
                    RecordCalendarMark.QUESTION -> ", 가족 질문"
                    RecordCalendarMark.PHOTO_FIRST, RecordCalendarMark.PHOTO_SECOND -> ", 활동 기록"
                    null -> ""
                }
            }, contentAlignment = Alignment.Center,
    ) {
        if (photo != null) RecordPhotoImage(photo, Modifier.fillMaxSize()) else when (mark) {
            RecordCalendarMark.QUESTION -> Image(painterResource(R.drawable.record_question), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            RecordCalendarMark.PLANNED -> Image(painterResource(R.drawable.record_clock_planned), null, Modifier.size(33.6.dp))
            RecordCalendarMark.CONFIRMED -> Image(painterResource(R.drawable.record_clock_confirmed), null, Modifier.size(33.6.dp))
            RecordCalendarMark.BUCKET -> Image(painterResource(R.drawable.record_bucket), null, Modifier.size(33.dp))
            RecordCalendarMark.PHOTO_FIRST, RecordCalendarMark.PHOTO_SECOND -> Unit
            null -> Unit
        }
        if (mark == null || mark == RecordCalendarMark.QUESTION || mark == RecordCalendarMark.PHOTO_FIRST || mark == RecordCalendarMark.PHOTO_SECOND) {
            Text(date.dayOfMonth.toString(), color = if (mark == RecordCalendarMark.PHOTO_SECOND) KiriColor.Text.Strong else KiriColor.Text.Primary, style = KiriDetailTypography.Body)
        }
        if (count > 1) Text("+${count - 1}", Modifier.align(Alignment.TopEnd).background(KiriColor.Bg.Secondary, RoundedCornerShape(4.dp)).padding(2.dp).testTag("record_day_count_$date"), color = KiriColor.Text.Strong, style = KiriDetailTypography.Small)
    }
}

@Composable
private fun ScheduleCard(schedule: RecordSchedule, onOpen: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    val confirmed = schedule.status == RecordScheduleStatus.CONFIRMED
    Surface(onClick = onOpen, color = KiriColor.Bg.Primary, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().testTag("record_schedule_${schedule.id}")) {
        Row(Modifier.heightIn(min = 110.dp).padding(horizontal = 18.dp, vertical = if (confirmed) 10.5.dp else 12.5.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = if (confirmed) KiriColor.Primary.Default.copy(alpha = .2f) else KiriColor.Bg.Elevated, shape = RoundedCornerShape(5.dp)) {
                    Text(schedule.status.label, style = KiriDetailTypography.Small.copy(lineHeight = if (confirmed) 21.sp else 17.sp), color = if (confirmed) KiriColor.Text.Accent else KiriColor.Text.Secondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                }
                Column {
                    Text(schedule.title, style = KiriDetailTypography.Title, color = KiriColor.Text.Strong)
                    Text("일시 I ${schedule.date.format(DateTimeFormatter.ofPattern("yy.MM.dd"))}", style = KiriDetailTypography.Small, color = KiriColor.Text.Secondary)
                }
            }
            Box(Modifier.size(24.dp)) {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.requiredSize(48.dp).align(Alignment.Center).testTag("record_schedule_menu_${schedule.id}")) {
                    Image(painterResource(R.drawable.record_more), "약속 메뉴", Modifier.size(10.dp, 2.dp))
                }
                DropdownMenu(menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("약속 보기") }, onClick = { menuExpanded = false; onOpen() })
                }
            }
        }
    }
}
