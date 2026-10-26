package com.kiri.feature.auth.model

import com.kiri.domain.model.term.Term
import com.kiri.feature.auth.login.SocialLoginResult

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface AuthUiEvent {
    data class LoginClicked(val provider: LoginProvider) : AuthUiEvent

    // 화면에서 실행한 SDK 로그인의 결과
    data class SocialLoginFinished(val result: SocialLoginResult) : AuthUiEvent

    // 약관 목록을 받지 못했을 때 다시 시도
    data object TermsRetryClicked : AuthUiEvent
    data object AllTermsToggled : AuthUiEvent
    data class TermToggled(val term: Term) : AuthUiEvent
    data class TermDetailClicked(val term: Term) : AuthUiEvent
    data object TermDetailDismissed : AuthUiEvent

    // 세부 내용 창의 동의하기
    data class TermDetailAgreed(val term: Term) : AuthUiEvent
    data object AgreeClicked : AuthUiEvent
    data object BackClicked : AuthUiEvent
}
