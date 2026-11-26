package com.kiri.feature.mypage.presentation.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kiri.feature.mypage.R
import com.kiri.feature.mypage.presentation.common.MypageDialogButton
import com.kiri.feature.mypage.presentation.common.MypageDialogButtonTone
import com.kiri.uikit.color.KiriColor
import java.time.LocalTime

// 정기 알림 발송 시각을 고르는 팝업 (Figma 시간설정 modal 11013:72775).
// 시(0~23)와 분(0~59) 칸을 위·아래로 넘겨 고르고, 저장을 눌러야 바뀐다. 취소·X·바깥 탭·뒤로가기는 그대로 닫는다
@Composable
internal fun NotificationTimeDialog(
    currentTime: LocalTime,
    currentTimeText: String,
    onSave: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    var hour by remember { mutableIntStateOf(currentTime.hour) }
    var minute by remember { mutableIntStateOf(currentTime.minute) }

    Dialog(
        onDismissRequest = onDismiss,
        // Figma 팝업은 화면 양옆에 22dp 만 남기므로 기본 너비 제한을 끈다
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .testTag("notification_time_dialog")
                .clip(RoundedCornerShape(16.dp))
                .background(KiriColor.Bg.Primary)
                .padding(horizontal = 22.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(42.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "설정한 시간에 알림을 보내드려요!",
                            style = TextStyle(
                                color = KiriColor.Text.Primary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 28.sp,
                                letterSpacing = (-0.04).em,
                            ),
                        )
                        Text(
                            text = "현재 알림 발송 시각 : $currentTimeText",
                            style = TextStyle(
                                color = KiriColor.Text.Accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 18.sp,
                                letterSpacing = (-0.04).em,
                            ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = onDismiss)
                            .testTag("notification_time_close"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(painter = painterResource(R.drawable.ic_mypage_close), contentDescription = "닫기")
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "설정 시간",
                        style = TextStyle(
                            color = KiriColor.Text.Primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 21.sp,
                            letterSpacing = (-0.04).em,
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeWheel(
                            count = 24,
                            initialValue = currentTime.hour,
                            unit = "시",
                            onValueChange = { hour = it },
                            testTag = "notification_time_hour",
                        )
                        TimeWheel(
                            count = 60,
                            initialValue = currentTime.minute,
                            unit = "분",
                            onValueChange = { minute = it },
                            testTag = "notification_time_minute",
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MypageDialogButton(
                    text = "취소",
                    onClick = onDismiss,
                    tone = MypageDialogButtonTone.NeutralOnPrimary,
                    testTag = "notification_time_cancel",
                )
                MypageDialogButton(
                    text = "저장",
                    onClick = { onSave(LocalTime.of(hour, minute)) },
                    tone = MypageDialogButtonTone.Accent,
                    testTag = "notification_time_save",
                )
            }
        }
    }
}

@Preview
@Composable
private fun NotificationTimeDialogPreview() {
    NotificationTimeDialog(
        currentTime = LocalTime.of(9, 0),
        currentTimeText = "오전 9시",
        onSave = {},
        onDismiss = {},
    )
}
