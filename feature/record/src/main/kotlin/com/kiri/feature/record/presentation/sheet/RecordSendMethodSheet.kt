package com.kiri.feature.record.presentation.sheet

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.feature.record.R
import com.kiri.uikit.KiriTheme
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriPretendard

// 요청 보내기를 누르면 뜨는 초대장 발송 방법 시트 (Figma 8286:69116).
// 줄 모양은 앱으로 보내기 8450:21439, 카카오톡으로 보내기 8450:21440 을 따른다
@Composable
fun RecordSendMethodSheet(
    onAppClick: () -> Unit,
    onKakaoClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    RecordModalSheet(onDismiss = onDismiss, testTag = "record_send_method_sheet", containerColor = KiriColor.Bg.Primary) {
        RecordSendMethodSheetContent(onAppClick, onKakaoClick, onDismiss)
    }
}

@Composable
internal fun RecordSendMethodSheetContent(
    onAppClick: () -> Unit,
    onKakaoClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(SheetHeight)) {
        Column(Modifier.padding(horizontal = 33.dp)) {
            Spacer(Modifier.height(48.dp))
            Text(
                text = "초대장 발송 방법을 선택해주세요",
                color = KiriColor.Text.Strong,
                style = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 30.sp, letterSpacing = (-0.8).sp),
            )
            Spacer(Modifier.height(33.dp))
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                // KIRI/Invite Option (8450:21439)
                RecordSendMethodRow(
                    icon = R.drawable.ic_record_send,
                    onClick = onAppClick,
                    testTag = "record_send_app",
                    texts = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("앱으로 보내기", color = KiriColor.Text.Strong, style = AppTitleStyle)
                            Text("앱 내에서 푸시 알람이 발송돼요!", color = KiriColor.Text.Tertiary, style = AppSubtitleStyle)
                        }
                    },
                    arrow = { RecordSendArrow(R.drawable.ic_record_invite_arrow, boxSize = 48.dp) },
                )
                // 8450:21440
                RecordSendMethodRow(
                    icon = R.drawable.ic_record_message,
                    onClick = onKakaoClick,
                    testTag = "record_send_kakao",
                    texts = {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("카카오톡으로 보내기", color = KiriColor.Text.Strong, style = KakaoTitleStyle)
                            Text("카카오톡으로 초대 링크를 발송해요", color = KiriColor.Text.Tertiary, style = KakaoSubtitleStyle)
                        }
                    },
                    arrow = { RecordSendArrow(R.drawable.ic_record_invite_arrow_small, boxSize = 24.dp) },
                )
            }
        }
        // 닫기 표시는 위 25, 오른쪽 30 위치의 19 크기. 누르는 영역만 48 로 넓힌다
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.5.dp, end = 15.5.dp)
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onCloseClick)
                .semantics { contentDescription = "닫기" }
                .testTag("record_send_close"),
            contentAlignment = Alignment.Center,
        ) {
            // Figma 8286:69143: 두께 3, 길이 24 막대
            RecordCloseMark(KiriColor.Icon.Secondary, strokeWidth = 3.dp, length = 24.dp)
        }
    }
}

// 324 x 87, 안쪽 내용은 276 폭으로 가운데 정렬
@Composable
private fun RecordSendMethodRow(
    icon: Int,
    onClick: () -> Unit,
    testTag: String,
    texts: @Composable () -> Unit,
    arrow: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(324.dp)
            .height(87.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KiriColor.Bg.Tertiary)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.width(276.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(KiriColor.Bg.Secondary, CircleShape), contentAlignment = Alignment.Center) {
                    Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
                }
                texts()
            }
            arrow()
        }
    }
}

@Composable
private fun RecordSendArrow(icon: Int, boxSize: Dp) {
    Box(Modifier.size(boxSize), contentAlignment = Alignment.Center) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.scale(scaleX = -1f, scaleY = 1f))
    }
}

private val SheetHeight = 388.dp

// KIRI/Title/Large, KIRI/Label/Small
private val AppTitleStyle = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 27.sp, letterSpacing = (-0.72).sp)
private val AppSubtitleStyle = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = (-0.48).sp)

// 8450:21440 은 글자 스타일 없이 자간 -2% 로 직접 지정돼 있다
private val KakaoTitleStyle = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 18.sp, letterSpacing = (-0.36).sp)
private val KakaoSubtitleStyle = TextStyle(fontFamily = KiriPretendard, fontSize = 12.sp, letterSpacing = (-0.24).sp)

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true, widthDp = 390)
@Composable
private fun RecordSendMethodSheetPreview() {
    KiriTheme {
        Box(Modifier.background(KiriColor.Bg.Primary, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))) {
            RecordSendMethodSheetContent({}, {}, {})
        }
    }
}
