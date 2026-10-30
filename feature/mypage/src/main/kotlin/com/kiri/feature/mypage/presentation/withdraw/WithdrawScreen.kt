package com.kiri.feature.mypage.presentation.withdraw

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.mypage.WithdrawViewModel
import com.kiri.feature.mypage.model.withdraw.WithdrawUiEffect
import com.kiri.feature.mypage.model.withdraw.WithdrawUiEvent
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriButtonTone
import com.kiri.uikit.component.KiriTopBar

private const val TAG = "WithdrawScreen"

// 탈퇴에 성공하면 로그인 화면으로 가고, 실패하면 알리고 화면에 남는다
@Composable
fun WithdrawRoute(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WithdrawViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val currentOnNavigateToLogin by rememberUpdatedState(onNavigateToLogin)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                WithdrawUiEffect.NavigateToLogin -> currentOnNavigateToLogin()
                WithdrawUiEffect.NavigateBack -> currentOnNavigateBack()
                is WithdrawUiEffect.ShowWithdrawError -> {
                    Log.w(TAG, "회원탈퇴 실패", effect.cause)
                    Toast.makeText(context, "탈퇴하지 못했어요. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    WithdrawScreen(
        onBackClick = { viewModel.onEvent(WithdrawUiEvent.BackClicked) },
        onWithdrawClick = { viewModel.onEvent(WithdrawUiEvent.WithdrawClicked) },
        modifier = modifier,
        isWithdrawing = state.isWithdrawing,
    )
}

// 회원탈퇴. 삭제되는 정보와 남는 기록을 안내하고 탈퇴할게요를 누르면 탈퇴한다
// isWithdrawing: 탈퇴 요청 중이면 탈퇴할게요 버튼을 막는다
@Composable
fun WithdrawScreen(
    onBackClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWithdrawing: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .testTag("withdraw_screen"),
    ) {
        KiriTopBar(onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "잠깐! 떠나기 전에 확인해주세요",
                style = TextStyle(
                    color = KiriColor.Text.Strong,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 1.4.em,
                    letterSpacing = (-0.02).em,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "탈퇴하면 계정은 삭제되지만,\n가족이 함께한 기록은 남을 수 있어요.",
                style = TextStyle(
                    color = KiriColor.Text.Tertiary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 1.4.em,
                    letterSpacing = (-0.04).em,
                ),
            )
            Spacer(Modifier.height(40.dp))
            BulletSection(
                title = "삭제되는 내 정보",
                items = listOf("계정 인증정보와 개인 프로필", "내 취향과 개인 설정", "가족공간의 내 참여 관계"),
            )
            Spacer(Modifier.height(36.dp))
            BulletSection(
                title = "가족과 함께한 기록",
                items = listOf(
                    "가족의 과거 일정과 완료 활동은 유지돼요.",
                    "기록의 작성자는 ‘탈퇴한 멤버’로 표시돼요.",
                    "가족 활동 맥락에 필요한 버킷 항목은 남아요.",
                ),
            )
            Spacer(Modifier.height(24.dp))
        }
        KiriButton(
            onClick = onWithdrawClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
            enabled = !isWithdrawing,
            text = "탈퇴할게요",
            tone = KiriButtonTone.Neutral,
            testTag = "withdraw_button",
        )
    }
}

@Composable
private fun BulletSection(
    title: String,
    items: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = TextStyle(
                color = KiriColor.Text.Primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 27.sp,
                letterSpacing = (-0.04).em,
            ),
        )
        Column {
            items.forEach { item ->
                Row {
                    // 목록 기호는 24dp 들여쓴 자리 가운데에 둔다 (Figma list-disc, ms-24)
                    Text(
                        text = "•",
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center,
                        style = bulletStyle,
                    )
                    Text(text = item, style = bulletStyle)
                }
            }
        }
    }
}

private val bulletStyle = TextStyle(
    color = KiriColor.Text.Primary,
    fontSize = 16.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 26.sp,
    letterSpacing = (-0.04).em,
)

@Preview(showSystemUi = true)
@Composable
private fun WithdrawScreenPreview() {
    WithdrawScreen(onBackClick = {}, onWithdrawClick = {})
}
