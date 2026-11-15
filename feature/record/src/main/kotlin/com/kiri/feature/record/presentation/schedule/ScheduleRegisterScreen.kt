package com.kiri.feature.record.presentation.schedule

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.feature.record.R
import com.kiri.feature.record.presentation.sheet.RecordMember
import com.kiri.feature.record.presentation.sheet.RecordMemberSelectSheet
import com.kiri.feature.record.presentation.sheet.RecordScheduleSheet
import com.kiri.uikit.KiriTheme
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriButtonTone
import com.kiri.uikit.component.KiriDetailTypography
import com.kiri.uikit.component.KiriPretendard
import com.kiri.uikit.component.KiriTopBar
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private enum class ScheduleRegisterSheet { Schedule, Member }

// 같은 화면을 새 일정 등록과 기존 일정 변경에 함께 쓴다
data class ScheduleFormMode(
    val appBarTitle: String,
    val submitText: String,
    // null 이면 저장한 사람·주간 미션 문구를 쓴다
    val subtitle: String? = null,
    // 제목 위 배지를 보여줄지. 일정 변경 화면은 Figma 에 배지가 없다 (Handoff 9898:21204)
    val showSubtitle: Boolean = true,
    val submitting: Boolean = false,
    val error: String? = null,
) {
    companion object {
        val Register = ScheduleFormMode(appBarTitle = "일정 등록하기", submitText = "초대장 만들기")

        fun edit(subtitle: String, submitting: Boolean, error: String?) = ScheduleFormMode(
            appBarTitle = "일정 변경하기",
            submitText = if (submitting) "변경 중…" else "일정 변경하기",
            subtitle = subtitle,
            showSubtitle = false,
            submitting = submitting,
            error = error,
        )
    }
}

// 저장한 기록으로 일정을 등록하는 화면 (Figma 8264:21250).
// 일정 선택 → 날짜선택 시트, 가족 구성원 선택 → 구성원선택 시트를 연다. 시트에서 고른 값은 다음/확인을 눌러야 반영된다
@Composable
fun ScheduleRegisterScreen(
    savedBy: String,
    title: String,
    description: String,
    members: List<RecordMember>,
    times: List<LocalTime>,
    draft: ScheduleRegisterDraft,
    onDraftChange: (ScheduleRegisterDraft) -> Unit,
    onBackClick: () -> Unit,
    onCreateInviteClick: () -> Unit,
    earliest: LocalDateTime? = null,
    weekly: Boolean = false,
    mode: ScheduleFormMode = ScheduleFormMode.Register,
    // 이미 일정이 있어 고를 수 없는 날과, 날짜 시트가 달을 보여줄 때 부르는 함수 (하루에 일정은 하나만 정할 수 있다)
    blockedDates: Set<java.time.LocalDate> = emptySet(),
    onMonthShown: (java.time.YearMonth) -> Unit = {},
) {
    var openSheet by rememberSaveable { mutableStateOf<ScheduleRegisterSheet?>(null) }

    ScheduleRegisterContent(
        savedBy = savedBy,
        title = title,
        description = description,
        draft = draft,
        onBackClick = onBackClick,
        onScheduleClick = { openSheet = ScheduleRegisterSheet.Schedule },
        onMemberClick = { openSheet = ScheduleRegisterSheet.Member },
        onMessageChange = { onDraftChange(draft.withMessage(it)) },
        onCreateInviteClick = onCreateInviteClick,
        weekly = weekly,
        mode = mode,
    )

    when (openSheet) {
        ScheduleRegisterSheet.Schedule -> {
            var date by rememberSaveable { mutableStateOf(draft.date) }
            var time by rememberSaveable { mutableStateOf(draft.time) }
            RecordScheduleSheet(
                selectedDate = date,
                selectedTime = time,
                times = times,
                onDateSelect = { date = it },
                onTimeSelect = { time = it },
                onNextClick = {
                    onDraftChange(draft.copy(date = date, time = time))
                    openSheet = null
                },
                onDismiss = { openSheet = null },
                earliest = earliest,
                blockedDates = blockedDates,
                onMonthShown = onMonthShown,
            )
        }
        ScheduleRegisterSheet.Member -> {
            var selectedIds by rememberSaveable { mutableStateOf(draft.memberIds.toList()) }
            RecordMemberSelectSheet(
                members = members,
                selectedIds = selectedIds.toSet(),
                onMemberToggle = { member ->
                    selectedIds = if (member.id in selectedIds) selectedIds - member.id else selectedIds + member.id
                },
                onConfirmClick = {
                    onDraftChange(draft.copy(memberIds = selectedIds.toSet()))
                    openSheet = null
                },
                onDismiss = { openSheet = null },
            )
        }
        null -> Unit
    }
}

@Composable
internal fun ScheduleRegisterContent(
    savedBy: String,
    title: String,
    description: String,
    draft: ScheduleRegisterDraft,
    onBackClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onMemberClick: () -> Unit,
    onMessageChange: (String) -> Unit,
    onCreateInviteClick: () -> Unit,
    weekly: Boolean = false,
    mode: ScheduleFormMode = ScheduleFormMode.Register,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("schedule_register_screen"),
    ) {
        KiriTopBar(onBackClick = onBackClick, title = mode.appBarTitle)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            ScheduleRegisterHeader(
                (mode.subtitle ?: if (weekly) "가족에게 추천된 주간 미션이에요!" else savedByLabel(savedBy)).takeIf { mode.showSubtitle },
                title,
                description,
            )
            Spacer(Modifier.height(30.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ScheduleFormSelectRow(
                    label = "일정 선택",
                    value = draft.date?.let { date -> draft.time?.let { scheduleLabel(date, it) } },
                    placeholder = "날짜와 시간을 선택해주세요",
                    onClick = onScheduleClick,
                    testTag = "schedule_register_schedule_row",
                )
                ScheduleFormSelectRow(
                    label = "가족 구성원 선택",
                    value = draft.memberIds.takeIf { it.isNotEmpty() }?.let { memberCountLabel(it.size) },
                    placeholder = "함께할 가족을 선택해주세요",
                    onClick = onMemberClick,
                    testTag = "schedule_register_member_row",
                )
                ScheduleMessageField(draft.message, onMessageChange)
            }
            mode.error?.let {
                Text(
                    it,
                    Modifier.padding(top = 12.dp).testTag("schedule_form_error"),
                    color = KiriColor.Status.Error,
                    style = KiriDetailTypography.Small,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        KiriButton(
            onClick = onCreateInviteClick,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
            enabled = draft.canSubmit && !mode.submitting,
            text = mode.submitText,
            tone = KiriButtonTone.Accent,
        )
    }
}

@Composable
private fun ScheduleRegisterHeader(subtitle: String?, title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Figma 는 rgba(155,234,81,0.2) 배경. 코드의 Bg.GreenSoft 는 0.1 이라 같은 0.2 값인 Primary.Container 를 쓴다
            if (subtitle != null) Text(
                text = subtitle,
                modifier = Modifier
                    .background(KiriColor.Primary.Container, RoundedCornerShape(5.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                color = KiriColor.Text.Accent,
                style = KiriDetailTypography.Small,
            )
            Text(title, color = KiriColor.Text.Strong, style = HeadingLarge)
        }
        if (description.isNotBlank()) Text(description, color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small)
    }
}

// KIRI/Form Select Row (8413:5614)
@Composable
private fun ScheduleFormSelectRow(
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)
            .clip(FieldShape)
            .background(KiriColor.Bg.Primary)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small)
            Text(
                text = value ?: placeholder,
                color = if (value != null) KiriColor.Text.Strong else KiriColor.Text.Tertiary,
                style = BodyLarge3,
                maxLines = 1,
            )
        }
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.ic_record_form_arrow),
                contentDescription = null,
                modifier = Modifier.scale(scaleX = -1f, scaleY = 1f),
            )
        }
    }
}

// 가족에게 한 줄 (8286:66914). 100자까지 적을 수 있고 줄이 늘면 상자도 같이 늘어난다
@Composable
private fun ScheduleMessageField(message: String, onMessageChange: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .background(KiriColor.Bg.Primary, FieldShape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text("가족에게 한 줄", color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = message,
            onValueChange = onMessageChange,
            modifier = Modifier.fillMaxWidth().testTag("schedule_register_message"),
            textStyle = MessageStyle.copy(color = KiriColor.Text.Strong),
            cursorBrush = SolidColor(KiriColor.Text.Accent),
            decorationBox = { field ->
                Box {
                    if (message.isEmpty()) Text("같이 가고싶은 가족들에게 한 줄 적어주세요!", color = KiriColor.Text.Primary, style = MessageStyle)
                    field()
                }
            },
        )
        Text(
            text = "${message.length}/${ScheduleRegisterDraft.MAX_MESSAGE_LENGTH}",
            modifier = Modifier.align(Alignment.End),
            color = KiriColor.Text.Secondary,
            style = KiriDetailTypography.Small,
        )
    }
}

private val FieldShape = RoundedCornerShape(16.dp)
private val MessageStyle = KiriDetailTypography.Body.copy(lineHeight = 24.sp)

// KIRI/Body/Large3
private val BodyLarge3 = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 30.sp, letterSpacing = (-0.8).sp)

// KIRI/Heading/Large. 프로젝트 폰트에 SemiBold 파일이 없어 Medium 을 굵게 합성해서 그린다
private val HeadingLarge = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.48).sp)

internal val DraftSaver: Saver<ScheduleRegisterDraft, Any> = listSaver(
    save = { listOf(it.date, it.time, ArrayList(it.memberIds), it.message) },
    restore = {
        @Suppress("UNCHECKED_CAST")
        ScheduleRegisterDraft(
            date = it[0] as LocalDate?,
            time = it[1] as LocalTime?,
            memberIds = (it[2] as List<String>).toSet(),
            message = it[3] as String,
        )
    },
)

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ScheduleRegisterContentPreview() {
    KiriTheme {
        ScheduleRegisterContent(
            savedBy = "아빠",
            title = "한강 뚜벅뚜벅 축제 가보기",
            description = "2026 한강 뚜벅뚜벅 축제는 잠수교를 보행교로 전환해 시민이 직접 걸으며 공연·체험·미식 등 다양한 문화·여가 프로그램을 즐기는 서울 대표 한강 축제입니다.",
            draft = ScheduleRegisterDraft(
                date = LocalDate.of(2026, 10, 7),
                time = LocalTime.of(16, 30),
                memberIds = setOf("1", "2", "3", "4"),
            ),
            onBackClick = {},
            onScheduleClick = {},
            onMemberClick = {},
            onMessageChange = {},
            onCreateInviteClick = {},
        )
    }
}
