package com.kiri.feature.home.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.domain.notification.*
import com.kiri.feature.home.R
import com.kiri.feature.home.ScheduleInvitationUiState
import com.kiri.feature.home.ScheduleInvitationViewModel
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.awaitCancellation

/** Full-screen window belongs to this route; normal detail navigation preserves the invitation underneath. */
@Composable
fun ScheduleInvitationRoute(onBack: () -> Unit, onDetails: (String) -> Unit,
    viewModel: ScheduleInvitationViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.reload(); awaitCancellation() }
    }
    Dialog(onDismissRequest = { if (!state.submitting) onBack() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        ScheduleInvitationScreen(state, onBack, { state.invitation?.schedule?.id?.let(onDetails) }, viewModel::confirm, viewModel::reload)
    }
}

@Composable
fun ScheduleInvitationScreen(state: ScheduleInvitationUiState, onBack: () -> Unit, onDetails: () -> Unit,
    onConfirm: () -> Unit, onRetry: () -> Unit) {
    val invitation = state.invitation
    val changed = invitation?.kind == InvitationKind.CHANGED
    Column(Modifier.fillMaxSize().background(KiriColor.Bg.Primary).safeDrawingPadding().testTag("schedule_invitation_screen")) {
        KiriAppBar(if (changed) "일정 변경알림" else "요청알림", onBack = { if (!state.submitting) onBack() })
        if (invitation != null) {
            Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 22.dp)) {
                Text("초대 수락 현황", style = KiriDetailTypography.Body, color = KiriColor.Text.Strong)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    invitation.members.forEach { member ->
                        val accepted = member.id in invitation.schedule.acceptedMemberIds || (member.id == invitation.currentUserId && invitation.accepted)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            KiriMemberAvatar(accepted, member.role, false, Modifier.testTag("invitation_member_${member.id}"),
                                completedDescription = "초대 수락", pendingDescription = "미수락")
                            Text(member.nickname, style = KiriDetailTypography.Body, color = KiriColor.Text.Primary)
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().background(KiriColor.Bg.Base)
            .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).testTag("invitation_content")) {
            if (state.loading && invitation == null) Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            if (state.confirmed) Text("약속이 확정되었어요", Modifier.padding(vertical = 12.dp).testTag("invitation_confirmed"), color = KiriColor.Text.Accent, style = KiriDetailTypography.Body)
            if (invitation != null) {
                Spacer(Modifier.height(36.dp))
                InvitationCard(invitation)
                Spacer(Modifier.height(18.dp))
                Surface(onClick = onDetails, color = KiriColor.Bg.Tertiary, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 45.dp).testTag("invitation_details")) {
                    Box(contentAlignment = Alignment.Center) { Text("자세히 보러가기", style = KiriDetailTypography.Body, color = KiriColor.Text.Primary) }
                }
                Spacer(Modifier.height(36.dp))
                Text("행사설명", style = KiriDetailTypography.Title, color = KiriColor.Text.Secondary)
                Text(invitation.mission.description.ifBlank { "등록된 행사 설명이 없어요." }, style = KiriDetailTypography.Body.copy(lineHeight = 26.sp), color = KiriColor.Text.Secondary)
                Spacer(Modifier.height(16.dp))
                Text("가족에게 한 줄", style = KiriDetailTypography.Body, color = KiriColor.Text.Accent)
                Spacer(Modifier.height(8.dp))
                Text(invitation.schedule.invitationMessage.ifBlank { "남긴 메시지가 없어요." }, style = KiriDetailTypography.Small.copy(lineHeight = 25.2.sp), color = KiriColor.Text.Secondary)
                Spacer(Modifier.height(24.dp))
            }
            state.error?.let { error ->
                Text(error, Modifier.padding(vertical = 12.dp).testTag("invitation_error"), color = KiriColor.Status.Error, style = KiriDetailTypography.Small)
                if (invitation == null) TextButton(onClick = onRetry, enabled = !state.loading) { Text("다시 시도") }
            }
        }
        Row(Modifier.fillMaxWidth().background(KiriColor.Bg.Base).padding(horizontal = 18.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KiriButton(onClick = onBack, text = "취소", tone = KiriButtonTone.Neutral, enabled = !state.submitting, modifier = Modifier.weight(1f))
            KiriButton(onClick = onConfirm, text = if (state.submitting) "확정 중…" else if (invitation?.accepted == true) "확정 완료" else "약속 확정하기",
                enabled = invitation != null && !invitation.accepted && !state.submitting && !state.loading, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun InvitationCard(invitation: ScheduleInvitation) {
    val changed = invitation.kind == InvitationKind.CHANGED
    Box(Modifier.fillMaxWidth().heightIn(min = 181.dp).testTag("invitation_card")) {
        Image(painterResource(R.drawable.kiri_invitation_envelope), null,
            Modifier.matchParentSize().layout { measurable, constraints ->
                // Figma's 364x189 export contains 10dp left and 8dp top shadow outside the 354x181 card.
                val left = 10.dp.roundToPx()
                val top = 8.dp.roundToPx()
                val artwork = measurable.measure(Constraints.fixed(constraints.maxWidth + left, constraints.maxHeight + top))
                layout(constraints.maxWidth, constraints.maxHeight) { artwork.place(-left, -top) }
            }, contentScale = ContentScale.FillBounds)
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 27.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("활동명", style = KiriDetailTypography.Small.copy(fontSize = 12.sp, lineHeight = 16.sp), color = KiriColor.Text.Tertiary)
            Spacer(Modifier.height(8.dp))
            Text(invitation.mission.summary.title, style = KiriDetailTypography.Title.copy(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold), color = KiriColor.Text.Strong, textAlign = TextAlign.Center)
            Spacer(Modifier.height(26.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Image(painterResource(if (changed) R.drawable.kiri_invitation_clock_changed else R.drawable.kiri_invitation_clock), null, Modifier.size(24.dp))
                    Text(invitation.schedule.startsAt.atZone(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yy.MM.dd")),
                        Modifier.testTag("invitation_date"), style = KiriDetailTypography.Small, color = if (changed) KiriColor.Text.Accent else KiriColor.Text.Secondary)
                }
                if (changed) Box(Modifier.align(Alignment.CenterStart).size(width = 98.75.dp, height = 34.dp).testTag("invitation_changed"), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.kiri_invitation_changed), null, Modifier.matchParentSize())
                    Text("변경되었어요!", style = KiriDetailTypography.Small, color = KiriColor.Text.Accent)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.kiri_invitation_people), null, Modifier.size(width = 16.8.dp, height = 19.2.dp)) }
                Text(invitation.members.joinToString(", ") { if (it.id == invitation.currentUserId) "나" else it.role },
                    style = KiriDetailTypography.Small, color = KiriColor.Text.Secondary)
            }
        }
    }
}
