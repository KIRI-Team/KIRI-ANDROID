package com.kiri.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.domain.model.term.Term
import com.kiri.feature.auth.presentation.component.AuthAgreementRow
import com.kiri.feature.auth.presentation.component.AuthAgreementRowType
import com.kiri.uikit.KiriTypography
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriButtonLayout
import com.kiri.uikit.component.KiriButtonTone
import com.kiri.uikit.component.KiriDetailSheet
import com.kiri.uikit.component.KiriLoadingIndicator
import com.kiri.uikit.component.KiriTitle
import com.kiri.uikit.component.KiriTopBar

@Composable
fun TermsAgreementScreen(
    terms: List<Term>,
    agreedTermIds: Set<Long>,
    allAgreed: Boolean,
    canContinue: Boolean,
    onAllToggle: () -> Unit,
    onTermToggle: (Term) -> Unit,
    onTermDetailClick: (Term) -> Unit,
    onAgreeClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoadingTerms: Boolean = false,
    termsLoadFailed: Boolean = false,
    onRetryClick: () -> Unit = {},
    viewingTerm: Term? = null,
    onTermDetailDismiss: () -> Unit = {},
    onTermDetailAgree: (Term) -> Unit = {},
) {
    Column(
        modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .systemBarsPadding()
            .testTag("terms_agreement_screen"),
    ) {
        KiriTopBar(onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            KiriTitle(
                title = "서비스 이용을 위해\n동의가 필요해요",
                subtitle = "필수 내용을 확인한 뒤 계속할 수 있어요",
                subtitleColor = KiriColor.Text.Tertiary,
            )
            Spacer(Modifier.height(86.dp))
            when {
                isLoadingTerms -> KiriLoadingIndicator(Modifier.align(Alignment.CenterHorizontally))
                termsLoadFailed -> TermsLoadFailed(onRetryClick = onRetryClick)
                else -> TermsAgreementRows(
                    terms = terms,
                    agreedTermIds = agreedTermIds,
                    allAgreed = allAgreed,
                    onAllToggle = onAllToggle,
                    onTermToggle = onTermToggle,
                    onTermDetailClick = onTermDetailClick,
                )
            }
        }
        KiriButton(
            onClick = onAgreeClick,
            text = "동의하기",
            enabled = canContinue,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
        )
    }
    viewingTerm?.let { term ->
        // 오른쪽 위 동의하기를 누르면 해당 약관에 동의하고 닫힌다
        KiriDetailSheet(
            title = term.agreementTitle,
            content = term.content,
            actionText = "동의하기",
            onActionClick = { onTermDetailAgree(term) },
            onDismiss = onTermDetailDismiss,
            testTag = "term_detail_sheet",
            actionTestTag = "term_detail_agree_button",
        )
    }
}

@Composable
private fun TermsAgreementRows(
    terms: List<Term>,
    agreedTermIds: Set<Long>,
    allAgreed: Boolean,
    onAllToggle: () -> Unit,
    onTermToggle: (Term) -> Unit,
    onTermDetailClick: (Term) -> Unit,
) {
    AuthAgreementRow(
        title = "전체동의",
        type = AuthAgreementRowType.All,
        checked = allAgreed,
        onCheckedChange = onAllToggle,
        testTag = "agreement_all",
    )
    Spacer(Modifier.height(15.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        terms.forEach { term ->
            AuthAgreementRow(
                title = term.agreementTitle,
                type = AuthAgreementRowType.Required,
                checked = term.id in agreedTermIds,
                onCheckedChange = { onTermToggle(term) },
                onDetailClick = { onTermDetailClick(term) },
                testTag = "agreement_term_${term.id}",
            )
        }
    }
}

// TODO: 약관 목록 로딩 실패 디자인이 정해지면 교체
@Composable
private fun TermsLoadFailed(onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
private fun TermsAgreementScreenPreview() {
    TermsAgreementScreen(
        terms = listOf(
            Term(id = 1, title = "서비스 이용약관", content = "", required = true),
            Term(id = 2, title = "개인정보 처리방침", content = "", required = true),
            Term(id = 3, title = "마케팅 정보 수신 동의", content = "", required = false),
        ),
        agreedTermIds = setOf(1),
        allAgreed = false,
        canContinue = false,
        onAllToggle = {},
        onTermToggle = {},
        onTermDetailClick = {},
        onAgreeClick = {},
        onBackClick = {},
    )
}
