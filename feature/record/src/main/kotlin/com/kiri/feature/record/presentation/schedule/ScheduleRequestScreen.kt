package com.kiri.feature.record.presentation.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriButtonTone
import com.kiri.uikit.component.KiriDetailTypography
import com.kiri.uikit.component.KiriPretendard
import com.kiri.uikit.component.KiriTopBar
import java.time.LocalDate

// 초대장 만들기를 누르면 보이는 요청알림 미리보기 (Figma 9424:23045). 가족이 받게 될 요청을 보여주고 보낼지 정한다.
// 일정 취소 확인(Figma 10629:27298)도 같은 구성이라 제목·안내 문구·버튼 문구만 바꿔 쓴다
@Composable
fun ScheduleRequestScreen(
    savedBy: String,
    title: String,
    description: String,
    date: LocalDate,
    memberNames: List<String>,
    message: String,
    onBackClick: () -> Unit,
    onDetailClick: () -> Unit,
    onSendClick: () -> Unit,
    sending: Boolean = false,
    error: String? = null,
    weekly: Boolean = false,
    topBarTitle: String = "요청알림",
    // 카드 위 안내 문구. null 이면 저장한 사람(주간 미션이면 추천) 문구를 쓴다
    headline: String? = null,
    dismissText: String = "취소",
    confirmText: String = "요청 보내기",
    confirmingText: String = "보내는 중…",
    // 요청알림은 Input 채움 색, 일정 취소는 KIRI/Button Neutral 기본 배경을 쓴다
    dismissBackground: Color = KiriComponentColor.Input.BgFilled,
) {
    BackHandler(onBack = onBackClick)
    Column(
        Modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("schedule_request_screen"),
    ) {
        KiriTopBar(onBackClick = onBackClick, title = topBarTitle)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = headline ?: if (weekly) "가족에게 추천된 주간 미션이에요!" else savedMissionLabel(savedBy),
                modifier = Modifier.fillMaxWidth(),
                color = KiriColor.Text.Primary,
                textAlign = TextAlign.Center,
                style = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = (-0.64).sp),
            )
            Spacer(Modifier.height(36.dp))
            ScheduleRequestCard(title, date, memberNames)
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KiriColor.Bg.Tertiary)
                    .clickable(role = Role.Button, onClick = onDetailClick)
                    .testTag("schedule_request_detail"),
                contentAlignment = Alignment.Center,
            ) {
                Text("자세히 보러가기", color = KiriColor.Text.Primary, style = TitleMedium)
            }
            // KIRI/Description Block (8424:5918). 설명이 없으면 블록째 숨긴다
            if (description.isNotBlank()) {
                Spacer(Modifier.height(36.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("행사설명", color = KiriColor.Text.Tertiary, style = KiriDetailTypography.Title)
                    Text(description, color = KiriColor.Text.Primary, style = KiriDetailTypography.Body)
                }
            }
            if (message.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("가족에게 한 줄", color = KiriColor.Text.Accent, style = TitleMedium.copy(lineHeight = 20.sp))
                    Text(message, color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small.copy(lineHeight = 1.8.em))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        error?.let {
            Text(
                text = it,
                modifier = Modifier.padding(horizontal = 18.dp).testTag("schedule_request_error"),
                color = KiriColor.Status.Error,
                style = KiriDetailTypography.Small,
            )
        }
        // KIRI/Bottom Action, Layout=Dual
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ScheduleRequestCancelButton(onBackClick, dismissText, dismissBackground, Modifier.weight(1f), enabled = !sending)
            KiriButton(
                onClick = onSendClick,
                modifier = Modifier.weight(1f),
                enabled = !sending,
                text = if (sending) confirmingText else confirmText,
                tone = KiriButtonTone.Accent,
            )
        }
    }
}

// 편지봉투 배경 위에 활동명, 날짜, 함께할 가족을 가운데 정렬로 올린다 (9424:23066 ~ 9424:23078)
@Composable
private fun ScheduleRequestCard(title: String, date: LocalDate, memberNames: List<String>) {
    Box(Modifier.fillMaxWidth().height(181.dp), contentAlignment = Alignment.TopCenter) {
        RequestEnvelope(Modifier.matchParentSize())
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(27.dp))
            Text(
                text = "활동명",
                color = KiriColor.Text.Tertiary,
                style = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = (-0.48).sp),
            )
            Spacer(Modifier.height(8.dp))
            // 프로젝트 폰트에 SemiBold 파일이 없어 Medium 을 굵게 합성해서 그린다
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = KiriColor.Text.Strong,
                textAlign = TextAlign.Center,
                style = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 20.sp, letterSpacing = (-0.96).sp),
            )
            Spacer(Modifier.height(26.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScheduleRequestInfo(R.drawable.ic_record_clock, requestDateLabel(date), gap = 4)
                ScheduleRequestInfo(R.drawable.ic_record_person, memberNames.joinToString(", "), gap = 5)
            }
        }
    }
}

// 편지봉투 배경 (Figma 9424:23066). 몸통 위에 덮개 삼각형을 얹고 묶음 전체를 30% 불투명도로 그린다.
// Figma 원본은 덮개 오른쪽 끝이 몸통보다 34 짧고 꼭짓점이 가운데에서 왼쪽으로 13.5 치우쳐 있어,
// 덮개가 카드 폭 전체를 덮고 꼭짓점이 가운데 오도록 바로잡았다. 색·깊이·그림자는 원본 값을 쓴다
@Composable
private fun RequestEnvelope(modifier: Modifier) {
    Canvas(modifier.graphicsLayer { alpha = 0.3f; compositingStrategy = CompositingStrategy.Offscreen }) {
        drawRect(KiriColor.Bg.Secondary)
        val flap = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, EnvelopeFlapDepth.toPx())
            close()
        }
        // 덮개 아래 그림자: y 2, 흐림 5 (Android 9 미만은 하드웨어 가속에서 그림자가 그려지지 않는다)
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = KiriColor.Bg.Tertiary.toArgb()
            setShadowLayer(8.dp.toPx(), 0f, 2.dp.toPx(), KiriColor.Bg.Base.toArgb())
        }
        drawIntoCanvas { it.nativeCanvas.drawPath(flap.asAndroidPath(), paint) }
    }
}

private val EnvelopeFlapDepth = 77.5.dp

@Composable
private fun ScheduleRequestInfo(icon: Int, text: String, gap: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap.dp)) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
        Text(
            text = text,
            color = KiriColor.Text.Secondary,
            style = TextStyle(fontFamily = KiriPretendard, fontSize = 14.sp, letterSpacing = (-0.7).sp),
        )
    }
}

// Figma 의 취소 버튼은 KIRI/Button Neutral 에 배경만 바꿔 쓴다(요청알림은 Input 채움 색 #2B2B2B). KiriButton 은 배경을 바꿀 수 없어 따로 그린다
@Composable
private fun ScheduleRequestCancelButton(onClick: () -> Unit, text: String, background: Color, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag("schedule_request_cancel"),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = KiriComponentColor.Button.FilledNeutral.ContentDefault, style = TitleMedium)
    }
}

// KIRI/Title/Medium
private val TitleMedium = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = (-0.64).sp)

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ScheduleRequestScreenPreview() {
    KiriTheme {
        ScheduleRequestScreen(
            savedBy = "딸",
            title = "한강 뚜벅뚜벅 축제 가보기",
            description = "2026 한강 뚜벅뚜벅 축제는 잠수교를 보행교로 전환해 시민이 직접 걸으며 공연·체험·미식 등 다양한 문화·여가 프로그램을 즐기는 서울 대표 한강 축제입니다.",
            date = LocalDate.of(2026, 6, 23),
            memberNames = listOf("엄마", "아빠", "나"),
            message = "엄마, 아빠 나 여기 가고싶은데 우리 10월 초에 갈래요??",
            onBackClick = {},
            onDetailClick = {},
            onSendClick = {},
        )
    }
}
