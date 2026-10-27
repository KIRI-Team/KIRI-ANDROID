package com.kiri.data.auth

import com.kiri.data.network.ApiErrorBody
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

// 토큰이 유효하지 않다는 401(AUTH401_1)을 받으면 refreshToken 으로 토큰을 다시 받아 같은 요청을 한 번 더 보낸다
// 재발급을 서버가 거절하면(4xx) 토큰을 지워 로그아웃 상태로 만든다. 연결 문제나 서버 오류(5xx)면 토큰을 남긴 채 실패만 돌려준다
@Singleton
internal class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val reissueApi: ReissueApi,
    private val json: Json,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        // 토큰 없이 보낸 요청, 이미 재시도한 요청, 토큰 문제가 아닌 401 은 재발급하지 않는다
        if (request.isNoAuth() || response.priorResponse != null || !response.isInvalidToken()) return null
        val sentToken = request.bearerToken() ?: return null

        // 여러 요청이 동시에 401 을 받아도 재발급은 한 번만 한다
        synchronized(lock) {
            val current = runBlocking { tokenStore.get() } ?: return null
            // 기다리는 동안 다른 요청이 이미 재발급했으면 새 토큰으로 바로 재시도한다
            if (current.accessToken != sentToken) return request.withBearer(current.accessToken)
            val reissued = reissue(current.refreshToken) ?: return null
            runBlocking { tokenStore.save(reissued) }
            return request.withBearer(reissued.accessToken)
        }
    }

    private fun reissue(refreshToken: String): AuthTokens? {
        val response = try {
            reissueApi.reissue(ReissueRequest(refreshToken)).execute()
        } catch (e: Exception) {
            // 연결 문제(IOException)나 응답 형식이 달라 읽지 못한 경우. 서버가 거절한 게 아니므로 토큰을 남긴다
            return null
        }
        val result = response.body()?.takeIf { it.isSuccess }?.result
        if (result != null) return AuthTokens(result.accessToken, result.refreshToken)
        if (response.isSuccessful || response.code() in 400..499) runBlocking { tokenStore.clear() }
        return null
    }

    // 원래 응답 본문은 그대로 두고 앞부분만 읽어 서버 오류 코드를 확인한다
    private fun Response.isInvalidToken(): Boolean {
        val body = runCatching { peekBody(MAX_ERROR_BODY_BYTES).string() }.getOrNull() ?: return false
        val error = runCatching { json.decodeFromString<ApiErrorBody>(body) }.getOrNull()
        return error?.code == INVALID_TOKEN_CODE
    }

    private companion object {
        // 만료된 토큰도 같은 코드로 온다
        const val INVALID_TOKEN_CODE = "AUTH401_1"
        const val MAX_ERROR_BODY_BYTES = 8 * 1024L
    }
}
