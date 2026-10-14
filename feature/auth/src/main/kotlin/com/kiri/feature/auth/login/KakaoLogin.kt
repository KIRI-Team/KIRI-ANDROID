package com.kiri.feature.auth.login

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.kiri.feature.auth.BuildConfig
import com.kiri.feature.auth.model.LoginProvider
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

// 카카오 로그인. 카카오톡이 있으면 카카오톡으로, 없거나 실패하면 카카오계정(웹)으로 로그인한다
// 서버에는 ID 토큰을 보내므로 카카오 디벨로퍼스에서 OpenID Connect 를 켜 두어야 한다
class KakaoLogin @Inject constructor() {

    suspend fun login(context: Context, nonce: String): SocialLoginResult {
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
            return SocialLoginResult.Failure(LoginProvider.KAKAO, MissingLoginKeyException("KAKAO_NATIVE_APP_KEY"))
        }
        val client = UserApiClient.instance
        if (client.isKakaoTalkLoginAvailable(context)) {
            val talkResult = loginWithKakaoTalk(context, nonce)
            // 카카오톡에서 사용자가 취소했으면 카카오계정 로그인으로 넘기지 않고 그대로 끝낸다
            if (!shouldFallbackToKakaoAccount(talkResult)) return talkResult
        }
        return loginWithKakaoAccount(context, nonce)
    }

    private suspend fun loginWithKakaoTalk(context: Context, nonce: String): SocialLoginResult =
        suspendCancellableCoroutine { cont ->
            UserApiClient.instance.loginWithKakaoTalk(context, nonce = nonce) { token, error ->
                cont.resume(kakaoLoginResult(token, error))
            }
        }

    private suspend fun loginWithKakaoAccount(context: Context, nonce: String): SocialLoginResult =
        suspendCancellableCoroutine { cont ->
            UserApiClient.instance.loginWithKakaoAccount(context, nonce = nonce) { token, error ->
                cont.resume(kakaoLoginResult(token, error))
            }
        }
}

// SDK 콜백(token, error)을 로그인 결과로 바꾼다
internal fun kakaoLoginResult(token: OAuthToken?, error: Throwable?): SocialLoginResult = when {
    error is ClientError && error.reason == ClientErrorCause.Cancelled -> SocialLoginResult.Cancelled
    error != null -> SocialLoginResult.Failure(LoginProvider.KAKAO, error)
    token == null -> SocialLoginResult.Failure(LoginProvider.KAKAO, IllegalStateException("카카오 로그인 결과가 비어 있습니다."))
    else -> token.idToken?.takeIf { it.isNotBlank() }
        ?.let { SocialLoginResult.Success(LoginProvider.KAKAO, it) }
        ?: SocialLoginResult.Failure(LoginProvider.KAKAO, MissingKakaoIdTokenException())
}

// 카카오톡 로그인 실패(카카오톡 미연결 계정 등)는 카카오계정 로그인으로 다시 시도하고, 성공·취소는 그대로 끝낸다
// ID 토큰이 없는 것은 앱 설정 문제라 카카오계정으로 다시 해도 같으므로 넘어가지 않는다
internal fun shouldFallbackToKakaoAccount(talkResult: SocialLoginResult): Boolean =
    talkResult is SocialLoginResult.Failure && talkResult.cause !is MissingKakaoIdTokenException

// 카카오가 ID 토큰을 주지 않았을 때 (카카오 디벨로퍼스의 OpenID Connect 가 꺼져 있음)
class MissingKakaoIdTokenException :
    IllegalStateException("카카오 ID 토큰이 없습니다. 카카오 디벨로퍼스에서 OpenID Connect 를 활성화하세요.")
