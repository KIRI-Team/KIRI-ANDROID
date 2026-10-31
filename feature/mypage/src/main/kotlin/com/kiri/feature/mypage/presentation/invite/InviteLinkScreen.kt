package com.kiri.feature.mypage.presentation.invite

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.domain.model.invite.Invite
import com.kiri.feature.mypage.InviteLinkViewModel
import com.kiri.feature.mypage.model.invite.InviteLinkUiEffect
import com.kiri.feature.mypage.model.invite.InviteLinkUiEvent
import com.kiri.feature.mypage.model.invite.InviteLinkUiState
import com.kiri.uikit.R as UikitR
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriButtonTone
import com.kiri.uikit.component.KiriInviteCodeCard
import com.kiri.uikit.component.KiriInviteCodeLoadingScreen
import com.kiri.uikit.component.KiriInviteCodeValue
import com.kiri.uikit.component.KiriTitle
import com.kiri.uikit.component.KiriTopBar
import com.kiri.uikit.invite.createInviteShareIntent
import java.time.LocalDate

@Composable
fun InviteLinkRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InviteLinkViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    // 공유 시트에서 돌아오면 결과와 관계없이 다시 공유할 수 있게 한다
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onEvent(InviteLinkUiEvent.InviteShared)
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is InviteLinkUiEffect.CopyToClipboard -> {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(effect.label, effect.text)))
                    // Android 13 이상은 시스템이 복사 알림을 직접 보여준다
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        Toast.makeText(context, "${effect.label}를 복사했어요", Toast.LENGTH_SHORT).show()
                    }
                }
                is InviteLinkUiEffect.ShareInvite ->
                    shareLauncher.launch(context.createInviteShareIntent(effect.url, effect.code))
                InviteLinkUiEffect.ShowInviteError ->
                    Toast.makeText(context, UikitR.string.invite_create_error, Toast.LENGTH_SHORT).show()
                InviteLinkUiEffect.ShowLoadError ->
                    Toast.makeText(context, "초대 정보를 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
                InviteLinkUiEffect.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    InviteLinkScreen(modifier = modifier, state = state, onEvent = viewModel::onEvent)
}

// 공용 초대 링크. 링크코드·초대링크를 복사하거나 공유하고, 리셋하기로 새 초대를 발급한다
@Composable
fun InviteLinkScreen(
    state: InviteLinkUiState,
    onEvent: (InviteLinkUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 발급하는 동안에는 시스템 뒤로가기로 나가지 않도록 막는다 (로딩 화면의 뒤로가기 버튼도 비활성화)
    BackHandler(enabled = state.isIssuing) {}

    if (state.isIssuing) {
        KiriInviteCodeLoadingScreen(modifier = modifier.testTag("invite_link_loading_screen"))
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .testTag("invite_link_screen"),
    ) {
        KiriTopBar(onBackClick = { onEvent(InviteLinkUiEvent.BackClicked) })
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            KiriTitle(
                title = "가족이 모이면 더 즐거워져요!",
                // TODO: 디자인 안내 문구가 주소 화면 문구로 들어가 있어 임시 문구를 쓴다. 디자인이 고쳐지면 교체
                subtitle = "링크코드나 초대링크를 가족에게 보내주세요",
                subtitleColor = KiriColor.Text.Tertiary,
                titleFontWeight = FontWeight.Normal,
            )
            Spacer(Modifier.height(86.dp))
            Column(
                modifier = Modifier.padding(horizontal = 21.5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.isReissueFailed) {
                    // TODO: 재발급 실패 화면 디자인이 나오면 교체 (지금은 코드·링크 자리에 임시 문구를 보여준다)
                    Text(
                        text = "코드 재발급에 실패했어요.\n리셋하기를 눌러 다시 발급해주세요.",
                        textAlign = TextAlign.Center,
                        style = captionStyle,
                        modifier = Modifier.testTag("invite_link_reissue_error"),
                    )
                    Spacer(Modifier.height(16.dp))
                } else {
                    InviteCodeCards(state = state, onEvent = onEvent)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(role = Role.Button) { onEvent(InviteLinkUiEvent.ResetClicked) }
                        .testTag("invite_link_reset"),
                ) {
                    Text(
                        text = "리셋하기",
                        textAlign = TextAlign.Center,
                        style = captionStyle.copy(textDecoration = TextDecoration.Underline),
                    )
                }
            }
        }
        KiriButton(
            onClick = { onEvent(InviteLinkUiEvent.ShareClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
            enabled = state.canShare,
            text = "가족에게 초대보내기",
            tone = KiriButtonTone.Neutral,
        )
    }
}

// 링크코드·초대링크 카드와 만료일
@Composable
private fun InviteCodeCards(
    state: InviteLinkUiState,
    onEvent: (InviteLinkUiEvent) -> Unit,
) {
    KiriInviteCodeCard(
        label = "링크코드",
        value = state.code,
        onCopyClick = { onEvent(InviteLinkUiEvent.CodeCopyClicked) },
        testTag = "invite_link_code",
    )
    Spacer(Modifier.height(16.dp))
    KiriInviteCodeCard(
        label = "초대링크",
        value = state.maskedLink,
        onCopyClick = { onEvent(InviteLinkUiEvent.LinkCopyClicked) },
        valueType = KiriInviteCodeValue.Link,
        testTag = "invite_link_url",
    )
    Spacer(Modifier.height(16.dp))
    state.expiresText?.let { expiresText ->
        Text(text = expiresText, style = captionStyle)
        Spacer(Modifier.height(4.dp))
    }
}

private val captionStyle = TextStyle(
    color = KiriColor.Text.Tertiary,
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 21.sp,
    letterSpacing = (-0.04).em,
)

@Preview(showSystemUi = true)
@Composable
private fun InviteLinkScreenPreview() {
    InviteLinkScreen(
        state = InviteLinkUiState(
            invite = Invite(
                code = "NWTKYJ",
                url = "https://kiri.example.com/invite/NWTKYJ",
                expiresAt = LocalDate.of(2026, 10, 12),
            ),
        ),
        onEvent = {},
    )
}
