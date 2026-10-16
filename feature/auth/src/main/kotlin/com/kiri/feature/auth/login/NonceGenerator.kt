package com.kiri.feature.auth.login

import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject

// 로그인마다 새로 만드는 일회용 값. SDK 에 넘기면 ID 토큰 안에 그대로 들어가고, 같은 값을 서버에도 보내
// 서버가 "이번 로그인에서 받은 토큰"인지 확인한다 (가로챈 토큰 재사용 방지)
class NonceGenerator @Inject constructor() {
    private val random = SecureRandom()

    fun create(): String {
        val bytes = ByteArray(NONCE_BYTES).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private companion object {
        const val NONCE_BYTES = 32
    }
}
