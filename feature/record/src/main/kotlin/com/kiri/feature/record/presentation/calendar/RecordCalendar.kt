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
        calendarWeeks(state.month).forEachIndexed { index, week ->
            item(key = "week-$index") {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { date ->
                        if (date == null) Spacer(Modifier.weight(1f).aspectRatio(1f))
                        else CalendarCell(date, state.calendarMarks[date], date == state.selectedDate,
                            { onEvent(RecordUiEvent.DateSelected(date)) }, Modifier.weight(1f))
                    }
                }
            }
        }
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
private fun CalendarCell(date: LocalDate, mark: RecordCalendarMark?, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val background = when {
        selected -> KiriColor.Primary.Default.copy(alpha = .2f)
        mark == RecordCalendarMark.QUESTION || mark == RecordCalendarMark.PHOTO_FIRST || mark == RecordCalendarMark.PHOTO_SECOND -> KiriColor.Bg.Base
        mark == RecordCalendarMark.PLANNED -> KiriColor.Bg.Secondary
        mark == RecordCalendarMark.CONFIRMED || mark == RecordCalendarMark.BUCKET -> KiriColor.Bg.Tertiary
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
        when (mark) {
            RecordCalendarMark.QUESTION -> Image(painterResource(R.drawable.record_question), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            RecordCalendarMark.PHOTO_FIRST -> Image(painterResource(R.drawable.record_photo_first), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            RecordCalendarMark.PHOTO_SECOND -> Image(painterResource(R.drawable.record_photo_second), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            RecordCalendarMark.PLANNED -> Image(painterResource(R.drawable.record_clock_planned), null, Modifier.size(33.6.dp))
            RecordCalendarMark.CONFIRMED -> Image(painterResource(R.drawable.record_clock_confirmed), null, Modifier.size(33.6.dp))
            RecordCalendarMark.BUCKET -> Image(painterResource(R.drawable.record_bucket), null, Modifier.size(33.dp))
            null -> Unit
        }
        if (mark == null || mark == RecordCalendarMark.QUESTION || mark == RecordCalendarMark.PHOTO_FIRST || mark == RecordCalendarMark.PHOTO_SECOND) {
            Text(date.dayOfMonth.toString(), color = if (mark == RecordCalendarMark.PHOTO_SECOND) KiriColor.Text.Strong else KiriColor.Text.Primary, style = KiriDetailTypography.Body)
        }
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
