package com.kiri.feature.record.presentation.sheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.feature.record.R
import com.kiri.uikit.KiriTheme
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.color.KiriComponentColor
import com.kiri.uikit.component.KiriPretendard
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// 일정 잡기에서 날짜와 시간을 고르는 시트 (Figma 8286:68225). 날짜와 시간을 모두 골라야 다음으로 넘어간다.
// earliest 를 주면 그보다 이른 날짜·시간은 흐리게 보이고 고를 수 없다
// 하루에 일정은 하나만 정할 수 있어 blockedDates(이미 일정이 있는 날)도 흐리게 보이고 고를 수 없다
// 보여주는 달이 바뀔 때마다 onMonthShown 으로 알려 그 달의 일정을 불러오게 한다
@Composable
fun RecordScheduleSheet(
    selectedDate: LocalDate?,
    selectedTime: LocalTime?,
    times: List<LocalTime>,
    onDateSelect: (LocalDate) -> Unit,
    onTimeSelect: (LocalTime) -> Unit,
    onNextClick: () -> Unit,
    onDismiss: () -> Unit,
    earliest: LocalDateTime? = null,
    initialMonth: YearMonth = selectedDate?.let(YearMonth::from) ?: earliest?.let(YearMonth::from) ?: YearMonth.now(),
    blockedDates: Set<LocalDate> = emptySet(),
    onMonthShown: (YearMonth) -> Unit = {},
) {
    var month by rememberSaveable { mutableStateOf(initialMonth) }
    val currentMonthShown by rememberUpdatedState(onMonthShown)
    LaunchedEffect(month) { currentMonthShown(month) }
    RecordModalSheet(onDismiss = onDismiss, testTag = "record_schedule_sheet") {
        RecordScheduleSheetContent(
            month = month,
            selectedDate = selectedDate,
            selectedTime = selectedTime,
            times = times,
            onMonthChange = { month = it },
            onDateSelect = onDateSelect,
            onTimeSelect = onTimeSelect,
            onNextClick = onNextClick,
            earliest = earliest,
            blockedDates = blockedDates,
        )
    }
}

@Composable
internal fun RecordScheduleSheetContent(
    month: YearMonth,
    selectedDate: LocalDate?,
    selectedTime: LocalTime?,
    times: List<LocalTime>,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onTimeSelect: (LocalTime) -> Unit,
    onNextClick: () -> Unit,
    earliest: LocalDateTime? = null,
    blockedDates: Set<LocalDate> = emptySet(),
) {
    // 날짜를 바꿔 이미 고른 시간이 지난 시간이 되면 고르지 않은 것으로 본다
    val timeAllowed = selectedTime != null && isSelectableTime(selectedDate, selectedTime, earliest)
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(31.dp))
        RecordSheetTitle("날짜선택")
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.align(Alignment.CenterHorizontally).width(343.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RecordMonthHeader(month, onMonthChange)
            RecordCalendar(month, selectedDate, earliest, blockedDates, onDateSelect)
        }
        Spacer(Modifier.height(29.dp))
        RecordSheetTitle("시간 선택")
        Spacer(Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(times) { time ->
                RecordTimeChip(
                    time = time,
                    selected = timeAllowed && time == selectedTime,
                    enabled = isSelectableTime(selectedDate, time, earliest),
                    onClick = { onTimeSelect(time) },
                )
            }
        }
        Spacer(Modifier.height(46.dp))
        // 고른 날에 뒤늦게 다른 일정이 있는 것으로 확인되면 다음으로 넘어가지 않는다
        RecordSheetBottomAction("다음", onNextClick, enabled = selectedDate != null && selectedDate !in blockedDates && timeAllowed)
    }
}

@Composable
private fun RecordMonthHeader(month: YearMonth, onMonthChange: (YearMonth) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = month.toRecordLabel(),
            modifier = Modifier.weight(1f).testTag("record_calendar_month"),
            color = KiriColor.Text.Strong,
            style = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 1.45.em, letterSpacing = (-0.72).sp),
        )
        RecordMonthArrow("이전 달", mirrored = false, testTag = "record_calendar_prev") { onMonthChange(month.minusMonths(1)) }
        RecordMonthArrow("다음 달", mirrored = true, testTag = "record_calendar_next") { onMonthChange(month.plusMonths(1)) }
    }
}

@Composable
private fun RecordMonthArrow(description: String, mirrored: Boolean, testTag: String, onClick: () -> Unit) {
    Image(
        painter = painterResource(R.drawable.ic_record_calendar_arrow),
        contentDescription = description,
        modifier = Modifier
            .size(28.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .scale(scaleX = if (mirrored) -1f else 1f, scaleY = 1f)
            .testTag(testTag),
    )
}

@Composable
private fun RecordCalendar(
    month: YearMonth,
    selectedDate: LocalDate?,
    earliest: LocalDateTime?,
    blockedDates: Set<LocalDate>,
    onDateSelect: (LocalDate) -> Unit,
) {
    val weeks = remember(month) { recordCalendarWeeks(month) }
    Column(Modifier.width(312.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
            WeekdayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.width(CellSize).padding(vertical = 2.dp),
                    color = KiriColor.Text.Disabled,
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = (-0.4).sp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            weeks.forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                    week.forEach { day ->
                        if (day == null) {
                            Spacer(Modifier.size(CellSize))
                        } else {
                            RecordCalendarCell(
                                day = day,
                                selected = day.isInMonth && day.date == selectedDate,
                                enabled = day.isInMonth && isSelectableDate(day.date, earliest) && day.date !in blockedDates,
                                onClick = { onDateSelect(day.date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// 다음 달 날짜는 달력을 채우는 용도로만 보여주고 고를 수 없다. 지난 날짜는 흐리게 보이고 고를 수 없다
@Composable
private fun RecordCalendarCell(day: RecordCalendarDay, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val selectable = if (enabled) {
        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(CellSize)
            .then(if (selected) Modifier.background(KiriComponentColor.Chip.BgSelected, RoundedCornerShape(8.dp)) else Modifier)
            .then(selectable)
            .testTag("record_calendar_day_${day.date}"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = when {
                selected -> KiriColor.Text.Accent
                day.isInMonth && !enabled -> KiriColor.Text.Disabled
                else -> KiriColor.Text.Primary
            },
            textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = (-0.64).sp),
        )
    }
}

// KIRI/Time Select (8450:20628 / 8450:20629)
@Composable
private fun RecordTimeChip(time: LocalTime, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .background(
                if (selected) KiriComponentColor.Chip.BgSelected else KiriColor.Bg.Tertiary,
                RoundedCornerShape(16.dp),
            )
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(10.dp)
            .testTag("record_time_$time"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = time.format(TimeFormat),
            color = when {
                selected -> KiriColor.Text.Accent
                !enabled -> KiriColor.Text.Disabled
                else -> KiriColor.Text.Primary
            },
            style = TextStyle(fontFamily = KiriPretendard, fontSize = 16.sp, letterSpacing = (-0.48).sp),
        )
    }
}

private val CellSize = 36.dp
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val CellGap = 10.dp
private val WeekdayLabels = listOf("월", "화", "수", "목", "금", "토", "일")

@Preview(backgroundColor = 0xFF2B2B2B, showBackground = true, widthDp = 390)
@Composable
private fun RecordScheduleSheetPreview() {
    KiriTheme {
        RecordScheduleSheetContent(
            month = YearMonth.of(2026, 12),
            selectedDate = LocalDate.of(2026, 12, 23),
            selectedTime = LocalTime.of(9, 30),
            times = (0 until 12).map { LocalTime.of(9, 0).plusMinutes(30L * it) },
            onMonthChange = {},
            onDateSelect = {},
            onTimeSelect = {},
            onNextClick = {},
        )
    }
}
