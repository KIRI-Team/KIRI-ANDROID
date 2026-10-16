package com.kiri.feature.auth.model

import com.kiri.domain.model.term.AgreementTerm
import com.kiri.feature.auth.login.SocialLoginResult

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface AuthUiEvent {
    data class LoginClicked(val provider: LoginProvider) : AuthUiEvent

    // 화면에서 실행한 SDK 로그인의 결과
    data class SocialLoginFinished(val result: SocialLoginResult) : AuthUiEvent
    data object AllTermsToggled : AuthUiEvent
    data class TermToggled(val term: AgreementTerm) : AuthUiEvent
    data class TermDetailClicked(val term: AgreementTerm) : AuthUiEvent
    data object TermDetailDismissed : AuthUiEvent

    // 세부 내용 창의 동의하기
    data class TermDetailAgreed(val term: AgreementTerm) : AuthUiEvent
    data object AgreeClicked : AuthUiEvent
    data object BackClicked : AuthUiEvent
}
