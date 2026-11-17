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
import com.kiri.uikit.component.KiriAsyncImage
import com.kiri.uikit.component.KiriParticipantStatus
import com.kiri.uikit.R as UikitR
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.kiri.domain.model.schedule.ScheduleChangedField
import java.time.LocalDate
import java.time.LocalTime

// 초대장 만들기를 누르면 보이는 요청알림 미리보기 (Figma 9424:23045). 가족이 받게 될 요청을 보여주고 보낼지 정한다.
// 일정 취소 확인(Figma 10629:27298)과 받은 초대에 응답하는 화면도 같은 구성이라 제목·안내 문구·버튼만 바꿔 쓴다
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
    // 보내기 전 미리보기라 받는 사람이 보는 "요청알림"이 아니라 "요청 미리보기"로 쓴다
    topBarTitle: String = "요청 미리보기",
    // 카드 위 안내 문구. null 이면 저장한 사람(주간 미션이면 추천) 문구를 쓴다
    headline: String? = null,
    dismissText: String = "취소",
    confirmText: String = "요청 보내기",
    confirmingText: String = "보내는 중…",
    // 요청알림은 Input 채움 색, 일정 취소는 KIRI/Button Neutral 기본 배경을 쓴다
    dismissBackground: Color = KiriComponentColor.Input.BgFilled,
    // 왼쪽 버튼. 기본은 뒤로가기와 같고, 응답 화면에서는 거절이다
    onDismissClick: () -> Unit = onBackClick,
    // 일정 변경알림에서 바뀐 항목. 날짜·시간이 바뀌면 일시 옆에, 참여자가 바뀌면 함께할 가족 옆에 "변경되었어요!" 배지를 단다
    changedFields: Set<ScheduleChangedField> = emptySet(),
    // 시간이 바뀌었을 때 일시에 함께 보여줄 시간
    time: LocalTime? = null,
    // 받은 요청알림·일정 변경알림의 초대 수락 현황 (Figma 10298:38588, 10298:38540). null 이면 숨기고 카드 위 안내 문구를 보여준다
    acceptance: List<KiriParticipantStatus>? = null,
    // 자세히 보러가기와 행사설명 사이 간격. 요청 화면은 36, 일정 취소 확인(Figma 10630:27955)은 30
    descriptionTopGap: Dp = 36.dp,
) {
    BackHandler(onBack = onBackClick)
    Column(
        Modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .navigationBarsPadding()
            .testTag("schedule_request_screen"),
    ) {
        // 수락 현황이 있으면 상태 표시줄부터 수락 현황까지 Card/BgDisabled 로 칠한다 (Figma 위쪽 258 영역)
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (acceptance != null) Modifier.background(KiriComponentColor.Card.BgDisabled) else Modifier)
                .statusBarsPadding(),
        ) {
            KiriTopBar(onBackClick = onBackClick, title = topBarTitle)
            if (acceptance != null) ScheduleAcceptanceStatus(acceptance)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            // 수락 현황이 있는 받은 알림 화면은 카드 위 안내 문구가 없다
            if (acceptance == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = headline ?: if (weekly) "가족에게 추천된 주간 미션이에요!" else savedMissionLabel(savedBy),
                    modifier = Modifier.fillMaxWidth(),
                    color = KiriColor.Text.Primary,
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = (-0.64).sp),
                )
            }
            Spacer(Modifier.height(36.dp))
            ScheduleRequestCard(title, date, memberNames, changedFields, time)
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
            // KIRI/Description Block (8424:5918). 설명이 없으면 블록째 숨긴다.
            // 행사 설명은 일정 등록 화면이 아니라 이 요청 화면에만 보여준다
            if (description.isNotBlank()) {
                Spacer(Modifier.height(descriptionTopGap))
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
            ScheduleRequestCancelButton(onDismissClick, dismissText, dismissBackground, Modifier.weight(1f), enabled = !sending)
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
private fun ScheduleRequestCard(
    title: String,
    date: LocalDate,
    memberNames: List<String>,
    changedFields: Set<ScheduleChangedField> = emptySet(),
    time: LocalTime? = null,
) {
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
                val dateChanged = ScheduleChangedField.DATE in changedFields || ScheduleChangedField.TIME in changedFields
                val dateText = if (ScheduleChangedField.TIME in changedFields) changedDateLabel(date, time) else requestDateLabel(date)
                ScheduleRequestInfo(R.drawable.ic_record_clock, dateText, gap = 4, changed = dateChanged, changedTag = "schedule_request_changed_date")
                ScheduleRequestInfo(
                    R.drawable.ic_record_person, memberNames.joinToString(", "), gap = 5,
                    changed = ScheduleChangedField.PARTICIPANTS in changedFields, changedTag = "schedule_request_changed_participants",
                )
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

// 바뀐 항목은 아이콘·글자를 Accent 로 칠하고 왼쪽에 "변경되었어요!" 말풍선을 단다 (Figma 10298:38570, 10298:38583).
// 말풍선은 아이콘 왼쪽 2 간격에 붙고, 아이콘·글자가 말풍선이 없을 때와 같은 가운데 자리에 오도록 오른쪽에 같은 폭을 비워 둔다
@Composable
private fun ScheduleRequestInfo(icon: Int, text: String, gap: Int, changed: Boolean = false, changedTag: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (changed) {
            ScheduleChangedBubble(Modifier.testTag(changedTag))
            Spacer(Modifier.width(ChangedBubbleGap))
        }
        Image(
            painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = if (changed) ColorFilter.tint(KiriColor.Text.Accent) else null,
        )
        Spacer(Modifier.width(gap.dp))
        Text(
            text = text,
            color = if (changed) KiriColor.Text.Accent else KiriColor.Text.Secondary,
            style = TextStyle(fontFamily = KiriPretendard, fontSize = 14.sp, letterSpacing = (-0.7).sp),
        )
        if (changed) Spacer(Modifier.width(ChangedBubbleWidth + ChangedBubbleGap))
    }
}

// 일정 변경알림에서 바뀐 항목 왼쪽에 다는 말풍선 (꼬리가 오른쪽 아이콘을 가리킨다)
@Composable
private fun ScheduleChangedBubble(modifier: Modifier = Modifier) {
    Box(modifier.size(ChangedBubbleWidth, 34.dp)) {
        Image(painterResource(R.drawable.record_changed_bubble), contentDescription = null, modifier = Modifier.matchParentSize())
        Text(
            text = "변경되었어요!",
            modifier = Modifier.padding(start = 10.dp, top = 8.dp),
            color = KiriColor.Text.Accent,
            style = TextStyle(fontFamily = KiriPretendard, fontSize = 14.sp, letterSpacing = (-0.7).sp),
            maxLines = 1,
        )
    }
}

private val ChangedBubbleWidth = 98.75.dp
private val ChangedBubbleGap = 2.dp

// 초대 수락 현황 (Figma 10298:38628, KIRI/Member Avatar Group). 상단바 아래 12, 아래쪽 20 여백
@Composable
private fun ScheduleAcceptanceStatus(participants: List<KiriParticipantStatus>) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 20.dp)
            .testTag("schedule_request_acceptance"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "초대 수락 현황",
            color = KiriColor.Text.Primary,
            style = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = (-0.64).sp),
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            participants.forEach { ScheduleAcceptanceAvatar(it) }
        }
    }
}

// 수락한 가족은 Primary 테두리, 아직이면 Border 테두리에 캐릭터를 흑백으로 흐리게 그린다
@Composable
private fun ScheduleAcceptanceAvatar(participant: KiriParticipantStatus) {
    Column(
        Modifier
            .width(44.dp)
            .semantics(mergeDescendants = true) { stateDescription = participant.status }
            .testTag("schedule_request_acceptance_${participant.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .border(1.dp, if (participant.accepted) KiriColor.Primary.Default else KiriColor.Border.Default, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(KiriColor.Bg.Secondary), contentAlignment = Alignment.Center) {
                val fallback = if (participant.accepted) UikitR.drawable.kiri_participant_accepted else UikitR.drawable.kiri_participant_pending
                val imageModifier = Modifier.size(27.dp).alpha(if (participant.accepted) 1f else 0.6f)
                val grayscale = if (participant.accepted) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                val imageUrl = participant.imageUrl
                if (imageUrl == null) {
                    Image(painterResource(fallback), null, imageModifier)
                } else {
                    KiriAsyncImage(imageUrl, null, imageModifier, ContentScale.Fit, colorFilter = grayscale) {
                        Image(painterResource(fallback), null, Modifier.matchParentSize())
                    }
                }
            }
        }
        Text(
            participant.name,
            color = KiriColor.Text.Primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontFamily = KiriPretendard, fontSize = 16.sp, lineHeight = 26.sp, letterSpacing = (-0.64).sp),
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
