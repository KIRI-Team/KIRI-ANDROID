package com.kiri.feature.record.presentation.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.domain.model.schedule.UpcomingSchedule
import com.kiri.domain.model.schedule.UpcomingScheduleType
import com.kiri.feature.record.R
import com.kiri.feature.record.model.*
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography
import java.time.format.DateTimeFormatter

// 다가오는 약속 카드. 응답이 필요한 일정은 회색 "응답 필요", 확정 일정은 초록 "약속 확정" 배지를 단다
// 일시 줄에는 날짜·시간과 남은 날(D-n, 당일은 D-Day)을 함께 보여준다
@Composable
internal fun ScheduleCard(schedule: UpcomingSchedule, onOpen: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    val confirmed = schedule.type != UpcomingScheduleType.RESPONSE_REQUIRED
    Surface(
        onClick = onOpen,
        color = KiriColor.Bg.Primary,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().testTag("record_schedule_${schedule.id}"),
    ) {
        Row(
            Modifier.heightIn(min = 110.dp)
                .padding(horizontal = 18.dp, vertical = if (confirmed) 10.5.dp else 12.5.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color =
                        if (confirmed) KiriColor.Primary.Default.copy(alpha = .2f)
                        else KiriColor.Bg.Elevated,
                    shape = RoundedCornerShape(5.dp),
                ) {
                    Text(
                        if (confirmed) "약속 확정" else "응답 필요",
                        style =
                            KiriDetailTypography.Small.copy(
                                lineHeight = if (confirmed) 21.sp else 17.sp
                            ),
                        color = if (confirmed) KiriColor.Text.Accent else KiriColor.Text.Secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
                Column {
                    Text(
                        schedule.title,
                        style = KiriDetailTypography.Title,
                        color = KiriColor.Text.Strong,
                    )
                    Text(
                        schedule.whenLabel(),
                        style = KiriDetailTypography.Small,
                        color = KiriColor.Text.Secondary,
                    )
                }
            }
            Box(Modifier.size(24.dp)) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier =
                        Modifier.requiredSize(48.dp)
                            .align(Alignment.Center)
                            .testTag("record_schedule_menu_${schedule.id}"),
                ) {
                    Image(
                        painterResource(R.drawable.record_more),
                        "약속 메뉴",
                        Modifier.size(10.dp, 2.dp),
                    )
                }
                DropdownMenu(menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("약속 보기") },
                        onClick = {
                            menuExpanded = false
                            onOpen()
                        },
                    )
                }
            }
        }
    }
}

// "일시 I 26.11.17 14:50 · D-2". 시간을 모르면 날짜만, 당일이면 D-Day 로 쓴다
internal fun UpcomingSchedule.whenLabel(): String {
    val time = time?.let { " ${it.format(DateTimeFormatter.ofPattern("HH:mm"))}" }.orEmpty()
    val dDay = if (type == UpcomingScheduleType.TODAY || dDay <= 0) "D-Day" else "D-$dDay"
    return "일시 I ${date.format(DateTimeFormatter.ofPattern("yy.MM.dd"))}$time · $dDay"
}
