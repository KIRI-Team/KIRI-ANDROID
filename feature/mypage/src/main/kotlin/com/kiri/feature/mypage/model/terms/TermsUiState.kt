package com.kiri.feature.mypage.model.terms

import com.kiri.domain.model.term.Term
import com.kiri.feature.mypage.model.common.toLastUpdatedText

data class TermsUiState(
    // 서버에서 받은 약관 목록 (회원가입 약관 동의와 같은 목록)
    val terms: List<Term> = emptyList(),
    val isLoading: Boolean = false,
    // 불러오지 못하면 다시 시도할 수 있다
    val loadFailed: Boolean = false,
    // 세부 내용을 보고 있는 약관 (null 이면 창이 닫혀 있다)
    val viewingTerm: Term? = null,
) {
    // 약관 중 가장 최근에 바뀐 날. 업데이트일을 하나도 모르면 표시하지 않는다
    val lastUpdatedText: String?
        get() = terms.mapNotNull { it.updatedAt }.maxOrNull()?.toLastUpdatedText()
}
