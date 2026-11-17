package com.kiri.feature.record.presentation.schedule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kiri.domain.record.ScheduleRequestChannel
import com.kiri.feature.record.presentation.sheet.RecordMember
import com.kiri.feature.record.presentation.sheet.RecordSendMethodSheet
import java.time.LocalDateTime
import java.time.LocalTime

// 고를 수 있는 시간. 서버에서 받기 전까지 09:00 ~ 21:00, 30분 단위로 둔다
internal val DefaultScheduleTimes: List<LocalTime> = (0..24).map { LocalTime.of(9, 0).plusMinutes(30L * it) }

// 일정 등록하기 → (초대장 만들기) → 요청알림 미리보기 → (요청 보내기) → 발송 방법 시트.
// 미리보기에서 취소나 뒤로가기를 누르면 적어 둔 내용 그대로 등록 화면으로 돌아온다
@Composable
internal fun ScheduleRegisterFlow(
    savedBy: String,
    title: String,
    description: String,
    members: List<RecordMember>,
    draft: ScheduleRegisterDraft,
    onDraftChange: (ScheduleRegisterDraft) -> Unit,
    onBackClick: () -> Unit,
    onDetailClick: () -> Unit,
    onSend: (ScheduleRequestChannel) -> Unit,
    sending: Boolean = false,
    error: String? = null,
    earliest: LocalDateTime? = null,
    times: List<LocalTime> = DefaultScheduleTimes,
    weekly: Boolean = false,
    // 이미 일정이 있어 고를 수 없는 날 (하루에 일정은 하나만 정할 수 있다)
    blockedDates: Set<java.time.LocalDate> = emptySet(),
    onMonthShown: (java.time.YearMonth) -> Unit = {},
) {
    var showRequest by rememberSaveable { mutableStateOf(false) }
    var showSendSheet by rememberSaveable { mutableStateOf(false) }
    val date = draft.date

    if (showRequest && date != null) {
        ScheduleRequestScreen(
            savedBy = savedBy,
            title = title,
            description = description,
            date = date,
            memberNames = members.filter { it.id in draft.memberIds }.map { it.name },
            message = draft.message,
            onBackClick = { if (!sending) showRequest = false },
            onDetailClick = onDetailClick,
            onSendClick = { showSendSheet = true },
            sending = sending,
            error = error,
            weekly = weekly,
        )
        if (showSendSheet) {
            RecordSendMethodSheet(
                onAppClick = {
                    showSendSheet = false
                    onSend(ScheduleRequestChannel.APP)
                },
                onKakaoClick = {
                    showSendSheet = false
                    onSend(ScheduleRequestChannel.KAKAO)
                },
                onDismiss = { showSendSheet = false },
            )
        }
    } else {
        ScheduleRegisterScreen(
            savedBy = savedBy,
            title = title,
            members = members,
            times = times,
            draft = draft,
            onDraftChange = onDraftChange,
            onBackClick = onBackClick,
            onCreateInviteClick = { showRequest = true },
            earliest = earliest,
            weekly = weekly,
            blockedDates = blockedDates,
            onMonthShown = onMonthShown,
        )
    }
}
