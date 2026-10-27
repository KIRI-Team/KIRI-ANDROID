package com.kiri.feature.auth.model

import com.kiri.domain.model.term.Term
import com.kiri.feature.auth.AuthStep

data class AuthUiState(
    val currentStep: AuthStep = AuthStep.LOGIN,
    // 서버에서 받은 약관 목록. 약관동의 단계에 들어갈 때 받는다
    val terms: List<Term> = emptyList(),
    val isLoadingTerms: Boolean = false,
    // 약관 목록을 받지 못했으면 다시 시도 버튼을 보여준다
    val termsLoadFailed: Boolean = false,
    // 동의한 약관 id
    val agreedTermIds: Set<Long> = emptySet(),
    // 세부 내용 창에서 보고 있는 약관 (null이면 닫힘)
    val viewingTerm: Term? = null,
    // SDK 로그인부터 서버 로그인이 끝날 때까지 true. 그동안 로그인 버튼을 막는다
    val isLoggingIn: Boolean = false,
    // 동의 내용을 서버에 저장하는 동안 true. 그동안 동의하기 버튼과 뒤로가기를 막는다
    val isAgreeing: Boolean = false,
    // 서버가 알려준 새 회원 여부. 약관동의 뒤 새 회원은 온보딩, 기존 회원은 메인으로 간다
    val isNewUser: Boolean = true,
) {
    val allAgreed: Boolean
        get() = terms.isNotEmpty() && terms.all { it.id in agreedTermIds }

    // 약관 목록을 받았고 필수 약관에 모두 동의해야 계속할 수 있다 (목록이 비어 있으면 계속할 수 없다)
    val canContinue: Boolean
        get() = terms.isNotEmpty() && !isAgreeing && terms.filter { it.required }.all { it.id in agreedTermIds }
}
