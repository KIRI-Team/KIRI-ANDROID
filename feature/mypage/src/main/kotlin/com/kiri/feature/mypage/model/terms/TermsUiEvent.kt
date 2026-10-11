package com.kiri.feature.mypage.model.terms

import com.kiri.domain.model.term.AgreementTerm

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface TermsUiEvent {
    data class TermClicked(val term: AgreementTerm) : TermsUiEvent

    // 확인 버튼, 끌어내리기, 바깥 탭, 뒤로가기로 세부 내용 창을 닫았다
    data object TermDetailClosed : TermsUiEvent
    data object BackClicked : TermsUiEvent
}
