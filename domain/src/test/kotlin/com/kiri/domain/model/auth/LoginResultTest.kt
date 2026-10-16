package com.kiri.domain.model.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginResultTest {

    @Test
    fun 새_회원은_약관동의를_거친다() {
        assertTrue(LoginResult(isNewUser = true, termsAgreed = false).needsTermsAgreement)
    }

    @Test
    fun 약관에_동의하지_않은_기존_회원도_약관동의를_거친다() {
        assertTrue(LoginResult(isNewUser = false, termsAgreed = false).needsTermsAgreement)
    }

    @Test
    fun 약관에_동의한_기존_회원은_약관동의를_건너뛴다() {
        assertFalse(LoginResult(isNewUser = false, termsAgreed = true).needsTermsAgreement)
    }
}
