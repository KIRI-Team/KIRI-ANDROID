package com.kiri.feature.auth.model

import com.kiri.feature.auth.AuthStep

data class AuthUiState(
    val currentStep: AuthStep = AuthStep.LOGIN,
    val agreedTerms: Set<AgreementTerm> = emptySet(),
) {
    val allAgreed: Boolean
        get() = agreedTerms.containsAll(AgreementTerm.entries)

    // 필수 약관에 모두 동의해야 계속할 수 있다
    val canContinue: Boolean
        get() = agreedTerms.containsAll(AgreementTerm.entries.filter { it.required })
}
