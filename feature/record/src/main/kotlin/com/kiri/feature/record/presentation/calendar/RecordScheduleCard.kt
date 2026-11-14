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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.domain.model.schedule.UpcomingSchedule
import com.kiri.domain.model.schedule.UpcomingScheduleType
import com.kiri.feature.record.R
import com.kiri.feature.record.model.*
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography
import java.time.format.DateTimeFormatter

// 다가오는 약속 카드 (Figma KIRI/Card/Calendar). 응답이 필요한 일정은 회색 "약속 예정", 확정 일정은 초록 "약속 확정" 배지를 단다
// 일시 줄은 Figma 처럼 날짜만 보여준다
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
                        if (confirmed) "약속 확정" else "약속 예정",
                        // 약속 예정은 KIRI/Label/Medium(굵기 Medium), 약속 확정은 Body/Medium
                        style =
                            if (confirmed) KiriDetailTypography.Small.copy(lineHeight = 21.sp)
                            else KiriDetailTypography.Small.copy(fontWeight = FontWeight.Medium, lineHeight = 17.sp),
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

// "일시 I 26.11.17"
internal fun UpcomingSchedule.whenLabel(): String =
    "일시 I ${date.format(DateTimeFormatter.ofPattern("yy.MM.dd"))}"
