package com.kiri.feature.record.presentation.schedule

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kiri.feature.record.presentation.sheet.RecordMember
import com.kiri.feature.record.presentation.sheet.RecordSendMethodSheet
import java.time.LocalTime

// 일정 등록하기 → (초대장 만들기) → 요청알림 미리보기 → (요청 보내기) → 발송 방법 시트.
// 미리보기에서 취소나 뒤로가기를 누르면 적어 둔 내용 그대로 등록 화면으로 돌아온다
@Composable
fun ScheduleRegisterRoute(
    savedBy: String,
    title: String,
    description: String,
    members: List<RecordMember>,
    times: List<LocalTime>,
    onBackClick: () -> Unit,
    onDetailClick: () -> Unit,
) {
    val context = LocalContext.current
    var draft by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf(ScheduleRegisterDraft()) }
    var showRequest by rememberSaveable { mutableStateOf(false) }
    var showSendSheet by rememberSaveable { mutableStateOf(false) }
    val date = draft.date

    if (showRequest && date != null) {
        val memberNames = members.filter { it.id in draft.memberIds }.map { it.name }
        ScheduleRequestScreen(
            savedBy = savedBy,
            title = title,
            description = description,
            date = date,
            memberNames = memberNames,
            message = draft.message,
            onBackClick = { showRequest = false },
            onDetailClick = onDetailClick,
            onSendClick = { showSendSheet = true },
        )
        if (showSendSheet) {
            RecordSendMethodSheet(
                onAppClick = {
                    showSendSheet = false
                    // TODO: 앱으로 보내기를 누른 뒤 이동할 화면이 정해지면 요청 전송과 함께 네비게이션 연결
                },
                onKakaoClick = {
                    showSendSheet = false
                    // 카카오톡을 포함해 설치된 앱을 고를 수 있는 시스템 공유 시트를 연다
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, scheduleRequestShareText(title, date, draft.time, memberNames, draft.message))
                    }
                    context.startActivity(Intent.createChooser(send, null))
                },
                onDismiss = { showSendSheet = false },
            )
        }
    } else {
        ScheduleRegisterScreen(
            savedBy = savedBy,
            title = title,
            description = description,
            members = members,
            times = times,
            draft = draft,
            onDraftChange = { draft = it },
            onBackClick = onBackClick,
            onCreateInviteClick = { showRequest = true },
        )
    }
}
