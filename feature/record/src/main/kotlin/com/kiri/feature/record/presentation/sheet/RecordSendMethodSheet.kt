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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.feature.record.R
import com.kiri.uikit.KiriTheme
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriPretendard

// 요청 보내기를 누르면 뜨는 초대장 발송 방법 시트 (Handoff 9898:18768).
// 두 줄 모두 KIRI/Card/Action Row (앱으로 보내기 = Email, 카카오톡 = Send 아이콘)
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
        Column(Modifier.padding(horizontal = 31.5.dp)) {
            Spacer(Modifier.height(48.dp))
            Text(
                text = "초대장 발송 방법을 선택해주세요",
                color = KiriColor.Text.Strong,
                style = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 30.sp, letterSpacing = (-0.8).sp),
            )
            Spacer(Modifier.height(33.dp))
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                RecordSendMethodRow(
                    icon = R.drawable.ic_record_email,
                    onClick = onAppClick,
                    testTag = "record_send_app",
                    texts = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("앱으로 보내기", color = KiriColor.Text.Strong, style = RowTitleStyle)
                            Text("앱 내에서 푸시 알람이 발송돼요!", color = KiriColor.Text.Tertiary, style = RowSubtitleStyle)
                        }
                    },
                )
                RecordSendMethodRow(
                    icon = R.drawable.ic_record_send,
                    onClick = onKakaoClick,
                    testTag = "record_send_kakao",
                    texts = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("카카오톡으로 보내기", color = KiriColor.Text.Strong, style = RowTitleStyle)
                            Text("카카오톡으로 초대 링크를 발송해요", color = KiriColor.Text.Tertiary, style = RowSubtitleStyle)
                        }
                    },
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

// KIRI/Card/Action Row: 390 폭에서 327 x 87(좌우 31.5), 배경 Bg.Secondary, 아이콘 원 Bg.Primary, 안쪽 여백 18
@Composable
private fun RecordSendMethodRow(
    icon: Int,
    onClick: () -> Unit,
    testTag: String,
    texts: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(87.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KiriColor.Bg.Secondary)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(KiriColor.Bg.Primary, CircleShape), contentAlignment = Alignment.Center) {
                    Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
                }
                texts()
            }
            // KIRI/Icon/Navigation/Back(24)을 좌우로 뒤집은 오른쪽 화살표
            Image(
                painterResource(R.drawable.ic_record_invite_arrow),
                contentDescription = null,
                modifier = Modifier.size(24.dp).scale(scaleX = -1f, scaleY = 1f),
            )
        }
    }
}

private val SheetHeight = 388.dp

// KIRI/Body/Large2, KIRI/Body/Small
private val RowTitleStyle = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 27.sp, letterSpacing = (-0.72).sp)
private val RowSubtitleStyle = TextStyle(fontFamily = KiriPretendard, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = (-0.48).sp)

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true, widthDp = 390)
@Composable
private fun RecordSendMethodSheetPreview() {
    KiriTheme {
        Box(Modifier.background(KiriColor.Bg.Primary, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))) {
            RecordSendMethodSheetContent({}, {}, {})
        }
    }
}
