package com.kiri.domain.model.auth

// 서버 로그인 결과. 토큰은 data 에서 보관하고, 화면에는 다음에 갈 곳을 정하는 값만 넘긴다
data class LoginResult(
    val isNewUser: Boolean,
    val termsAgreed: Boolean,
) {
    // 새 회원이거나 아직 약관에 동의하지 않았으면 약관동의부터 거친다
    val needsTermsAgreement: Boolean
        get() = isNewUser || !termsAgreed
}
