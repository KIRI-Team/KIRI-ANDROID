package com.kiri.feature.auth.login

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.kiri.feature.auth.BuildConfig
import com.kiri.feature.auth.model.LoginProvider
import javax.inject.Inject

// 구글 로그인. 로그인 버튼으로 시작하므로 Credential Manager 의 "Google로 로그인" 창을 띄우고 ID 토큰을 받는다
class GoogleLogin @Inject constructor() {

    private companion object {
        const val TAG = "GoogleLogin"
    }

    suspend fun login(activity: Activity, nonce: String): SocialLoginResult {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            return SocialLoginResult.Failure(LoginProvider.GOOGLE, MissingLoginKeyException("GOOGLE_WEB_CLIENT_ID"))
        }
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).setNonce(nonce).build())
            .build()
        return try {
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                SocialLoginResult.Success(LoginProvider.GOOGLE, GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                SocialLoginResult.Failure(
                    LoginProvider.GOOGLE,
                    IllegalStateException("예상하지 못한 로그인 정보입니다: ${credential.type}"),
                )
            }
        } catch (e: GetCredentialException) {
            // 구글 설정 문제(SHA-1·클라이언트 ID 불일치 등)도 취소로 올 때가 있어 원인 확인용으로 남긴다
            Log.w(TAG, "구글 로그인 종료: ${e.type} ${e.errorMessage}", e)
            googleLoginFailure(e)
        } catch (e: GoogleIdTokenParsingException) {
            googleLoginFailure(e)
        }
    }
}

// 로그인 창을 닫은 것은 취소로, 나머지(계정 없음, 네트워크, 토큰 해석 실패 등)는 실패로 본다
internal fun googleLoginFailure(error: Exception): SocialLoginResult = when (error) {
    is GetCredentialCancellationException -> SocialLoginResult.Cancelled
    else -> SocialLoginResult.Failure(LoginProvider.GOOGLE, error)
}
