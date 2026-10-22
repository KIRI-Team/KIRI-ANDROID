package com.kiri.feature.schedule

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.domain.home.ScheduleStatus
import com.kiri.domain.schedule.*
import com.kiri.feature.detail.model.DetailUiState
import com.kiri.feature.detail.model.DetailUiEvent
import com.kiri.feature.detail.presentation.MissionDetailScreen
import com.kiri.uikit.KiriTypography
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.*
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ScheduleRoute(onBack: () -> Unit, onReview: (String) -> Unit, viewModel: ScheduleViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner, viewModel) { owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        viewModel.reload()
        while (true) { viewModel.tick(); delay(1000) }
    } }
    val back = { if (!viewModel.back()) onBack() }
    BackHandler(onBack = back)
    ScheduleScreen(state, back, viewModel::change, viewModel::setDateTime, viewModel::preview,
        { external -> viewModel.send { schedule -> if (external) {
            val text = "${schedule.title}\n${scheduleLabel(schedule.startsAt)}\n${schedule.place}\n${schedule.message}\nKIRI 앱의 일정 상세에서 수락 또는 거절해주세요."
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "초대장 공유"))
        } } }, viewModel::respond, viewModel::edit, viewModel::cancel, onReview, viewModel::reload)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    state: ScheduleUiState, onBack: () -> Unit, onChange: (ScheduleDraft) -> Unit,
    onDateTime: (LocalDate, LocalTime) -> Unit, onPreview: () -> Unit, onSend: (Boolean) -> Unit,
    onRespond: (MemberResponse) -> Unit, onEdit: () -> Unit, onCancel: () -> Unit,
    onReview: (String) -> Unit, onRetry: () -> Unit,
) {
    var choosingDate by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var choosingTime by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(KiriColor.Bg.Base).imePadding().testTag("schedule_screen")) {
        KiriDetailAppBar(if (state.preview) "초대장 미리보기" else if (state.editing) "약속 만들기" else "약속 상세", onBack)
        if (state.loading) CircularProgressIndicator(Modifier.padding(24.dp))
        state.error?.let { Text(it, Modifier.padding(18.dp).testTag("schedule_error"), color = KiriColor.Status.Error, style = KiriTypography.BodyMedium) }
        state.notice?.let { Text(it, Modifier.padding(horizontal = 18.dp), color = KiriColor.Text.Accent, style = KiriTypography.BodyMedium) }
        if (state.mission == null) {
            if (!state.loading) TextButton(onRetry) { Text("다시 시도") }
        } else if (state.editing) {
            val draft = state.draft ?: return
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(state.mission.summary.title, style = KiriDetailTypography.Display)
                if (state.preview) {
                    Text(scheduleLabel(draft.startsAt), style = KiriTypography.Value)
                    Text(draft.place, style = KiriTypography.BodyLarge)
                    Text("초대할 가족", style = KiriTypography.TitleLarge)
                    Text(state.family?.members.orEmpty().filter { it.id in draft.inviteeIds }.joinToString { it.nickname }, style = KiriTypography.BodyLarge)
                    Text("제안자는 자동으로 참여해요.", style = KiriTypography.BodyMedium)
                    if (draft.message.isNotBlank()) Text(draft.message, style = KiriTypography.BodyLarge)
                    Text("요청을 보내면 일정이 생성됩니다. 외부 공유를 취소해도 만들어진 일정은 유지돼요.", style = KiriTypography.BodyMedium, color = KiriColor.Text.Secondary)
                } else {
                    KiriFormSelectRow("일정 선택", draft.startsAt?.let(::scheduleLabel) ?: "날짜와 시간을 선택해주세요", { choosingDate = true }, Modifier.testTag("schedule_date"), !state.busy)
                    Text("초대할 가족", style = KiriTypography.TitleLarge)
                    state.family?.members.orEmpty().filter { it.accessible && it.id != state.family?.currentUserId }.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { member -> KiriFamilySelectionCard(member.nickname, member.id in draft.inviteeIds,
                                { onChange(draft.copy(inviteeIds = if (member.id in draft.inviteeIds) draft.inviteeIds - member.id else draft.inviteeIds + member.id)) },
                                Modifier.weight(1f).testTag("schedule_invitee_${member.id}"), !state.busy) }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    OutlinedTextField(draft.place, { onChange(draft.copy(place = it)) }, label = { Text("장소") }, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().testTag("schedule_place"), textStyle = KiriTypography.BodyLarge)
                    Text(state.mission.scheduleWindow?.let { "행사 운영기간: ${it.firstDate} ~ ${it.lastDate}" } ?: "방문 전 공식 운영시간과 휴무일을 확인해주세요.", style = KiriTypography.BodyMedium, color = KiriColor.Text.Secondary)
                    OutlinedTextField(draft.message, { onChange(draft.copy(message = it.take(500))) }, label = { Text("가족에게 한 줄 (선택)") }, enabled = !state.busy,
                        supportingText = { Text("${draft.message.length}/500") }, modifier = Modifier.fillMaxWidth().testTag("schedule_message"), textStyle = KiriTypography.BodyLarge)
                }
                Spacer(Modifier.height(16.dp))
            }
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.preview) {
                    KiriButton({ onSend(false) }, enabled = !state.busy, text = if (state.schedule == null) "앱으로 요청 보내기" else "수정 완료", layout = KiriButtonLayout.Record, testTag = "schedule_send")
                    if (state.schedule == null) KiriButton({ onSend(true) }, enabled = !state.busy, text = "요청 생성 후 외부 공유", tone = KiriButtonTone.Neutral, layout = KiriButtonLayout.Record)
                } else KiriButton(onPreview, enabled = state.canPreview, text = "초대장 만들기", layout = KiriButtonLayout.Record, testTag = "schedule_preview")
            }
        } else state.schedule?.let { schedule ->
            val userId = state.family?.currentUserId.orEmpty()
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(when (schedule.status(state.now)) { ScheduleStatus.CONFIRMED -> "약속 확정"; ScheduleStatus.COMPLETED -> "완료"; ScheduleStatus.CANCELLED -> "취소"; ScheduleStatus.NOT_CONFIRMED -> "미확정"; else -> "응답 대기" }, style = KiriTypography.TitleLarge, color = KiriColor.Text.Accent, modifier = Modifier.testTag("schedule_status"))
                Text("내 응답: ${schedule.responses[userId]?.label ?: "초대받지 않은 가족 일정"}", style = KiriTypography.BodyMedium, modifier = Modifier.testTag("schedule_personal_response"))
                Text("응답 마감: ${scheduleLabel(schedule.responseDeadline)}", style = KiriTypography.BodySmall)
                KiriParticipantStatusButton(schedule.responses.map { (id, response) -> KiriParticipantStatus(id, state.family?.members?.find { it.id == id && it.accessible }?.nickname ?: "탈퇴한 멤버", response == MemberResponse.ACCEPTED, response.label) })
                if (schedule.message.isNotBlank()) Text(schedule.message, style = KiriTypography.BodyMedium)
            }
            val mission = state.mission
            MissionDetailScreen(DetailUiState(detail = com.kiri.domain.detail.MissionDetail(
                schedule.missionId, schedule.title, state.family?.familyName.orEmpty(), schedule.startsAt.atZone(ZoneId.of("Asia/Seoul")).toLocalDate(),
                scheduleLabel(schedule.startsAt), schedule.place, mission.expectedSteps, mission.durationMinutes, schedule.acceptedIds.map { id -> com.kiri.domain.detail.DetailParticipant(state.family?.members?.find { it.id == id && it.accessible }?.nickname ?: "탈퇴한 멤버", true) }, schedule.completed, mission.activities, mission.essentials, mission.nearby)),
                onEvent = { if (it == DetailUiEvent.BackClicked) onBack() },
                calendarHeader = com.kiri.feature.detail.presentation.CalendarDetailHeader("", "약속 정보", false), showAppBar = false, showTabs = false,
                modifier = Modifier.weight(1f))
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (schedule.canRespond(userId, state.now)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KiriButton({ onRespond(MemberResponse.DECLINED) }, Modifier.weight(1f), !state.busy, "거절", KiriButtonTone.Neutral, layout = KiriButtonLayout.Record, testTag = "schedule_decline")
                    KiriButton({ onRespond(MemberResponse.ACCEPTED) }, Modifier.weight(1f), !state.busy, "수락", layout = KiriButtonLayout.Record, testTag = "schedule_accept")
                }
                if (schedule.canReview(userId, state.now)) KiriButton({ onReview(schedule.id) }, text = "오늘의 추억 기록하기", layout = KiriButtonLayout.Record, testTag = "schedule_review")
                if (schedule.proposerId == userId && !schedule.cancelled && !schedule.completed) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onEdit, enabled = !state.busy, modifier = Modifier.testTag("schedule_edit")) { Text("수정하기") }
                    TextButton({ confirmCancel = true }, enabled = !state.busy, modifier = Modifier.testTag("schedule_cancel")) { Text("약속 취소") }
                }
            }
        }
    }
    if (choosingDate) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = state.draft?.startsAt?.atZone(ZoneId.of("Asia/Seoul"))?.toLocalDate()?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            selectableDates = object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long): Boolean = !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(state.now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate()) })
        DatePickerDialog(onDismissRequest = { choosingDate = false }, confirmButton = { TextButton({ selectedDate = dateState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }; choosingDate = false; choosingTime = selectedDate != null }, enabled = dateState.selectedDateMillis != null) { Text("시간 선택") } }, dismissButton = { TextButton({ choosingDate = false }) { Text("취소") } }) { DatePicker(dateState) }
    }
    if (choosingTime) {
        val time = rememberTimePickerState(is24Hour = true)
        AlertDialog(onDismissRequest = { choosingTime = false }, title = { Text("시간 선택") }, text = { TimeInput(time) }, confirmButton = { TextButton({ selectedDate?.let { onDateTime(it, LocalTime.of(time.hour, time.minute)) }; choosingTime = false }) { Text("선택") } }, dismissButton = { TextButton({ choosingTime = false }) { Text("취소") } })
    }
    if (confirmCancel) AlertDialog(onDismissRequest = { confirmCancel = false }, title = { Text("약속을 취소할까요?") }, text = { Text("가족에게 취소 내역이 표시되고 연결된 버킷은 다시 일정 잡기가 가능해져요.") }, confirmButton = { TextButton({ confirmCancel = false; onCancel() }) { Text("약속 취소") } }, dismissButton = { TextButton({ confirmCancel = false }) { Text("돌아가기") } })
}

private fun scheduleLabel(time: Instant?): String = time?.atZone(ZoneId.of("Asia/Seoul"))?.format(DateTimeFormatter.ofPattern("yy.MM.dd(E) · HH:mm", Locale.KOREAN)) ?: "미정"
