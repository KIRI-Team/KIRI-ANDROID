package com.kiri.feature.record.presentation.schedule

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.feature.record.R
import com.kiri.feature.record.presentation.sheet.RecordCloseMark
import com.kiri.feature.record.presentation.sheet.RecordModalSheet
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.color.KiriComponentColor
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriButtonTone
import com.kiri.uikit.component.KiriPretendard
import java.time.LocalDate

// 받은 초대에서 약속 확정하기를 누르면 뜨는 확인 시트 (Figma KIRI/Bottom Sheet 9898:18767)
@Composable
fun ScheduleAcceptSheet(
    title: String,
    date: LocalDate,
    memberNames: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    RecordModalSheet(onDismiss = onDismiss, testTag = "schedule_accept_sheet", containerColor = KiriColor.Bg.Primary) {
        ScheduleAcceptSheetContent(title, date, memberNames, onConfirm, onDismiss)
    }
}

@Composable
internal fun ScheduleAcceptSheetContent(
    title: String,
    date: LocalDate,
    memberNames: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 48.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("캘린더에 일정을 추가하시겠습니까?", color = KiriColor.Text.Strong, style = HeadingMedium)
                // 활동명·날짜·함께할 가족 카드
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(KiriColor.Bg.Tertiary, RoundedCornerShape(10.dp))
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("활동명", color = KiriColor.Text.Tertiary, style = BodySmall)
                        Text(title, color = KiriColor.Text.Strong, style = HeadingLarge)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        AcceptInfo(R.drawable.ic_record_clock, requestDateLabel(date), gap = 4)
                        AcceptInfo(R.drawable.ic_record_person, memberNames.joinToString(", "), gap = 5)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "초대된 가족 구성원에게도 알림이 발송됩니다",
                modifier = Modifier.fillMaxWidth(),
                color = KiriColor.Text.Secondary,
                style = BodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(39.dp))
            // KIRI/Bottom Action (Layout=Dual). 취소는 Input 채움 색 배경
            Row(
                Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KiriButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    text = "취소",
                    tone = KiriButtonTone.Neutral,
                    defaultContainerColor = KiriComponentColor.Input.BgFilled,
                    testTag = "schedule_accept_cancel",
                )
                KiriButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    text = "확인",
                    testTag = "schedule_accept_confirm",
                )
            }
        }
        // 닫기 표시는 위 25, 오른쪽 30 위치의 19 크기. 누르는 영역만 48 로 넓힌다
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.5.dp, end = 15.5.dp)
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onDismiss)
                .semantics { contentDescription = "닫기" }
                .testTag("schedule_accept_close"),
            contentAlignment = Alignment.Center,
        ) {
            RecordCloseMark(KiriColor.Bg.Elevated)
        }
    }
}

@Composable
private fun AcceptInfo(icon: Int, text: String, gap: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap.dp)) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
        Text(text, color = KiriColor.Text.Secondary, style = BodyMedium)
    }
}

// KIRI/Heading/Medium, Heading/Large, Body/Medium, Body/Small
private val HeadingMedium = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.8).sp)
private val HeadingLarge = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.48).sp)
private val BodyMedium = TextStyle(fontFamily = KiriPretendard, fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = (-0.56).sp)
private val BodySmall = TextStyle(fontFamily = KiriPretendard, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = (-0.48).sp)
