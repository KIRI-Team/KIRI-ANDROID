package com.kiri.data.auth

import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import retrofit2.Invocation

// 로그인 토큰이 필요한 요청에 Authorization 헤더를 붙인다. @NoAuth 가 붙은 API 는 건너뛴다
internal class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.isNoAuth()) return chain.proceed(request)
        val accessToken = runBlocking { tokenStore.get() }?.accessToken ?: return chain.proceed(request)
        return chain.proceed(request.withBearer(accessToken))
    }
}

// API 메서드에 붙이면 토큰 없이 요청한다 (로그인, 토큰 재발급)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class NoAuth

internal fun Request.isNoAuth(): Boolean =
    tag(Invocation::class.java)?.method()?.isAnnotationPresent(NoAuth::class.java) == true

internal fun Request.withBearer(accessToken: String): Request =
    newBuilder().header(AUTHORIZATION, "$BEARER$accessToken").build()

internal fun Request.bearerToken(): String? = header(AUTHORIZATION)?.removePrefix(BEARER)

private const val AUTHORIZATION = "Authorization"
private const val BEARER = "Bearer "
