package com.kiri.feature.auth.model

import com.kiri.domain.model.term.AgreementTerm
import com.kiri.feature.auth.AuthStep

data class AuthUiState(
    val currentStep: AuthStep = AuthStep.LOGIN,
    val agreedTerms: Set<AgreementTerm> = emptySet(),
    // 세부 내용 창에서 보고 있는 약관 (null이면 닫힘)
    val viewingTerm: AgreementTerm? = null,
    // SDK 로그인부터 서버 로그인이 끝날 때까지 true. 그동안 로그인 버튼을 막는다
    val isLoggingIn: Boolean = false,
    // 서버가 알려준 새 회원 여부. 약관동의 뒤 새 회원은 온보딩, 기존 회원은 메인으로 간다
    val isNewUser: Boolean = true,
) {
    val allAgreed: Boolean
        get() = agreedTerms.containsAll(AgreementTerm.entries)

    // 필수 약관에 모두 동의해야 계속할 수 있다
    val canContinue: Boolean
        get() = agreedTerms.containsAll(AgreementTerm.entries.filter { it.required })
}
