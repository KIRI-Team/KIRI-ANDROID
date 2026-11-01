package com.kiri.feature.mypage.presentation.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.domain.model.term.Term
import com.kiri.feature.mypage.TermsViewModel
import com.kiri.feature.mypage.model.terms.TermsUiEffect
import com.kiri.feature.mypage.model.terms.TermsUiEvent
import com.kiri.feature.mypage.model.terms.TermsUiState
import com.kiri.feature.mypage.presentation.common.MypageMenuCard
import com.kiri.feature.mypage.presentation.common.MypageMenuRow
import com.kiri.feature.mypage.presentation.common.lastUpdatedStyle
import com.kiri.uikit.KiriTypography
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriButtonLayout
import com.kiri.uikit.component.KiriButtonTone
import com.kiri.uikit.component.KiriDetailSheet
import com.kiri.uikit.component.KiriLoadingIndicator
import com.kiri.uikit.component.KiriTopBar
import java.time.LocalDate

@Composable
fun TermsRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TermsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                TermsUiEffect.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    TermsScreen(modifier = modifier, state = state, onEvent = viewModel::onEvent)
}

// 이용 약관. 약관을 누르면 회원가입 때와 같은 세부 내용 창을 띄운다
@Composable
fun TermsScreen(
    state: TermsUiState,
    onEvent: (TermsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .testTag("terms_screen"),
    ) {
        KiriTopBar(onBackClick = { onEvent(TermsUiEvent.BackClicked) }, title = "이용 약관")
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "세부 약관",
                style = TextStyle(
                    color = KiriColor.Text.Primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 27.sp,
                    letterSpacing = (-0.04).em,
                ),
            )
            Spacer(Modifier.height(16.dp))
            when {
                state.isLoading -> KiriLoadingIndicator(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 24.dp),
                )
                state.loadFailed -> TermsLoadFailed(onRetryClick = { onEvent(TermsUiEvent.RetryClicked) })
                else -> {
                    MypageMenuCard {
                        state.terms.forEach { term ->
                            MypageMenuRow(
                                title = term.title,
                                testTag = "terms_item_${term.id}",
                                onClick = { onEvent(TermsUiEvent.TermClicked(term)) },
                                titleColor = KiriColor.Text.Secondary,
                            )
                        }
                    }
                    state.lastUpdatedText?.let { lastUpdatedText ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = lastUpdatedText,
                            modifier = Modifier.align(Alignment.End),
                            style = lastUpdatedStyle,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    state.viewingTerm?.let { term ->
        // 회원가입 약관 창과 같고, 동의하기 대신 확인을 누르면 닫힌다
        KiriDetailSheet(
            title = term.title,
            content = term.content,
            actionText = "확인",
            onActionClick = { onEvent(TermsUiEvent.TermDetailClosed) },
            onDismiss = { onEvent(TermsUiEvent.TermDetailClosed) },
            testTag = "terms_detail_sheet",
            actionTestTag = "terms_detail_confirm",
        )
    }
}

// 회원가입 약관 동의 화면과 같은 임시 실패 표시
// TODO: 약관 목록 로딩 실패 디자인이 정해지면 교체
@Composable
private fun TermsLoadFailed(onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .testTag("terms_load_failed"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "약관을 불러오지 못했어요",
            style = KiriTypography.BodyMedium.copy(color = KiriColor.Text.Tertiary),
        )
        KiriButton(
            onClick = onRetryClick,
            text = "다시 시도",
            tone = KiriButtonTone.Neutral,
            layout = KiriButtonLayout.Inline,
            testTag = "terms_retry_button",
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun TermsScreenPreview() {
    TermsScreen(
        state = TermsUiState(
            terms = listOf(
                Term(id = 1, title = "서비스 이용약관", content = "", required = true, updatedAt = LocalDate.of(2026, 10, 4)),
                Term(id = 2, title = "개인정보 수집 및 이용 동의", content = "", required = true),
            ),
        ),
        onEvent = {},
    )
}
