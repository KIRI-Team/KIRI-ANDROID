package com.kiri.feature.place.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kiri.domain.place.PlaceFilters
import com.kiri.domain.time.kiriToday
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.*
import java.time.LocalDate
import java.time.YearMonth

/** A separate draft: back/dismiss never commits a partial date/time selection. */
@Composable
internal fun PlaceDatePicker(
    applied: PlaceFilters,
    onBack: () -> Unit,
    onApply: (PlaceFilters) -> Unit,
) {
    val today = com.kiri.uikit.LocalKiriClock.current.kiriToday()
    var draft by rememberSaveable(stateSaver = PlaceFiltersSaver) { mutableStateOf(applied) }
    var monthDay by rememberSaveable {
        mutableLongStateOf(
            LocalDate.ofEpochDay(applied.dateEpochDay ?: today.toEpochDay())
                .withDayOfMonth(1)
                .toEpochDay()
        )
    }
    val month = YearMonth.from(LocalDate.ofEpochDay(monthDay))
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier.fillMaxSize()
                .background(KiriColor.Bg.Base)
                .safeDrawingPadding()
                .testTag("filter_date_picker")
        ) {
            KiriAppBar("날짜 선택", onBack = onBack)
            Column(
                Modifier.weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 23.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${month.year}년 ${month.monthValue}월",
                        Modifier.weight(1f),
                        style = KiriDetailTypography.Title,
                        color = KiriColor.Text.Strong,
                    )
                    TextButton(
                        onClick = { monthDay = month.minusMonths(1).atDay(1).toEpochDay() },
                        modifier = Modifier.testTag("filter_month_previous"),
                    ) {
                        Text("‹", color = KiriColor.Text.Secondary)
                    }
                    TextButton(
                        onClick = { monthDay = month.plusMonths(1).atDay(1).toEpochDay() },
                        modifier = Modifier.testTag("filter_month_next"),
                    ) {
                        Text("›", color = KiriColor.Text.Secondary)
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                        Box(
                            Modifier.weight(1f).height(28.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day,
                                style = KiriDetailTypography.Small,
                                color = KiriColor.Text.Tertiary,
                            )
                        }
                    }
                }
                val first = month.atDay(1).minusDays((month.atDay(1).dayOfWeek.value % 7).toLong())
                val weeks = (month.lengthOfMonth() + month.atDay(1).dayOfWeek.value % 7 + 6) / 7
                repeat(weeks) { week ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { weekday ->
                            val date = first.plusDays((week * 7 + weekday).toLong())
                            val selected = draft.dateEpochDay == date.toEpochDay()
                            Box(
                                Modifier.weight(1f).height(42.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    onClick = {
                                        draft = draft.copy(dateEpochDay = date.toEpochDay())
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color =
                                        if (selected) KiriColor.Primary.Default
                                        else KiriColor.Bg.Base,
                                    modifier =
                                        Modifier.size(36.dp).testTag("filter_day_$date").semantics {
                                            this.selected = selected
                                        },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "${date.dayOfMonth}",
                                            style = KiriDetailTypography.Small,
                                            color =
                                                if (selected) KiriColor.Text.Inverse
                                                else if (YearMonth.from(date) == month)
                                                    KiriColor.Text.Primary
                                                else KiriColor.Text.Disabled,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(48.dp))
                FilterHeading("시간 선택")
                LazyRow(
                    Modifier.fillMaxWidth().padding(top = 16.dp).testTag("filter_times"),
                    state =
                        rememberLazyListState(
                            initialFirstVisibleItemIndex = applied.minutesOfDay / 30
                        ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(48) { slot ->
                        val minutes = slot * 30
                        FilterChip(
                            formatFilterTime(minutes),
                            draft.minutesOfDay == minutes,
                            { draft = draft.copy(minutesOfDay = minutes) },
                            "filter_time_$minutes",
                        )
                    }
                }
                Spacer(Modifier.height(48.dp))
                FilterHeading("인원 선택")
                Row(
                    Modifier.padding(top = 16.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (1..10).forEach { count ->
                        FilterChip(
                            "${count}명",
                            draft.people == count,
                            { draft = draft.copy(people = count) },
                            "filter_people_$count",
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            FilterActions(
                { draft = draft.copy(dateEpochDay = null, minutesOfDay = 570, people = 2) },
                { onApply(draft) },
                draft.dateEpochDay != null,
                tagPrefix = "date_filter",
            )
        }
    }
}
