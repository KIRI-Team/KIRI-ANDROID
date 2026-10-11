package com.kiri.feature.mypage.model.terms

import com.kiri.domain.model.term.AgreementTerm
import com.kiri.feature.mypage.model.common.toLastUpdatedText

data class TermsUiState(
    val terms: List<AgreementTerm> = AgreementTerm.entries,
    // 세부 내용을 보고 있는 약관 (null 이면 창이 닫혀 있다)
    val viewingTerm: AgreementTerm? = null,
) {
    val lastUpdatedText: String
        get() = AgreementTerm.LAST_UPDATED.toLastUpdatedText()
}
