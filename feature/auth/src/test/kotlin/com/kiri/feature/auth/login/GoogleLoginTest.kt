package com.kiri.feature.auth.login

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.kiri.feature.auth.model.LoginProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleLoginTest {

    @Test
    fun 로그인_창을_닫으면_취소로_본다() {
        assertEquals(SocialLoginResult.Cancelled, googleLoginFailure(GetCredentialCancellationException("closed")))
    }

    @Test
    fun 기기에_구글_계정이_없으면_실패로_보고_원인을_담는다() {
        val error = NoCredentialException("no account")

        assertEquals(SocialLoginResult.Failure(LoginProvider.GOOGLE, error), googleLoginFailure(error))
    }
}
