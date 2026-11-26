package com.kiri.feature.auth.login

import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kiri.feature.auth.model.LoginProvider
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KakaoLoginTest {

    private fun token(idToken: String?) = OAuthToken(
        accessToken = "access",
        accessTokenExpiresAt = Date(),
        refreshToken = "refresh",
        refreshTokenExpiresAt = Date(),
        idToken = idToken,
    )

    @Test
    fun 토큰을_받으면_ID_토큰으로_성공한다() {
        assertEquals(
            SocialLoginResult.Success(LoginProvider.KAKAO, "kakao-id-token"),
            kakaoLoginResult(token("kakao-id-token"), null),
        )
    }

    @Test
    fun ID_토큰이_없으면_설정_문제로_실패하고_카카오계정_로그인으로_넘어가지_않는다() {
        val result = kakaoLoginResult(token(idToken = null), null)

        assertTrue((result as SocialLoginResult.Failure).cause is MissingKakaoIdTokenException)
        assertFalse(shouldFallbackToKakaoAccount(result))
    }

    @Test
    fun 사용자가_취소하면_취소로_본다() {
        val result = kakaoLoginResult(null, ClientError(ClientErrorCause.Cancelled, "cancelled"))

        assertEquals(SocialLoginResult.Cancelled, result)
    }

    @Test
    fun 그_밖의_오류는_실패로_보고_원인을_담는다() {
        val error = ClientError(ClientErrorCause.NotSupported, "not supported")

        assertEquals(SocialLoginResult.Failure(LoginProvider.KAKAO, error), kakaoLoginResult(null, error))
    }

    @Test
    fun 토큰과_오류가_모두_없으면_실패로_본다() {
        assertTrue(kakaoLoginResult(null, null) is SocialLoginResult.Failure)
    }

    @Test
    fun 카카오톡_로그인이_실패했을_때만_카카오계정_로그인으로_넘어간다() {
        assertTrue(shouldFallbackToKakaoAccount(SocialLoginResult.Failure(LoginProvider.KAKAO, RuntimeException())))
        assertFalse(shouldFallbackToKakaoAccount(SocialLoginResult.Cancelled))
        assertFalse(shouldFallbackToKakaoAccount(SocialLoginResult.Success(LoginProvider.KAKAO, "token")))
    }
}
