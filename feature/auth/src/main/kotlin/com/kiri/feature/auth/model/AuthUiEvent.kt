package com.kiri.feature.auth.model

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface AuthUiEvent {
    data class LoginClicked(val provider: LoginProvider) : AuthUiEvent
    data object AllTermsToggled : AuthUiEvent
    data class TermToggled(val term: AgreementTerm) : AuthUiEvent
    data class TermDetailClicked(val term: AgreementTerm) : AuthUiEvent
    data object AgreeClicked : AuthUiEvent
    data object BackClicked : AuthUiEvent
}
