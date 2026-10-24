package com.kiri.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.domain.model.term.AgreementTerm
import com.kiri.feature.auth.presentation.component.AuthAgreementRow
import com.kiri.feature.auth.presentation.component.AuthAgreementRowType
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriDetailSheet
import com.kiri.uikit.component.KiriTitle
import com.kiri.uikit.component.KiriTopBar

@Composable
fun TermsAgreementScreen(
    agreedTerms: Set<AgreementTerm>,
    allAgreed: Boolean,
    canContinue: Boolean,
    onAllToggle: () -> Unit,
    onTermToggle: (AgreementTerm) -> Unit,
    onTermDetailClick: (AgreementTerm) -> Unit,
    onAgreeClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewingTerm: AgreementTerm? = null,
    onTermDetailDismiss: () -> Unit = {},
    onTermDetailAgree: (AgreementTerm) -> Unit = {},
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
            AuthAgreementRow(
                title = "전체동의",
                type = AuthAgreementRowType.All,
                checked = allAgreed,
                onCheckedChange = onAllToggle,
                testTag = "agreement_all",
            )
            Spacer(Modifier.height(15.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AgreementTerm.entries.forEach { term ->
                    AuthAgreementRow(
                        title = term.title,
                        type = AuthAgreementRowType.Required,
                        checked = term in agreedTerms,
                        onCheckedChange = { onTermToggle(term) },
                        onDetailClick = { onTermDetailClick(term) },
                        testTag = "agreement_${term.name.lowercase()}",
                    )
                }
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
            title = term.title,
            content = term.content,
            actionText = "동의하기",
            onActionClick = { onTermDetailAgree(term) },
            onDismiss = onTermDetailDismiss,
            testTag = "term_detail_sheet",
            actionTestTag = "term_detail_agree_button",
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun TermsAgreementScreenPreview() {
    TermsAgreementScreen(
        agreedTerms = emptySet(),
        allAgreed = false,
        canContinue = false,
        onAllToggle = {},
        onTermToggle = {},
        onTermDetailClick = {},
        onAgreeClick = {},
        onBackClick = {},
    )
}
