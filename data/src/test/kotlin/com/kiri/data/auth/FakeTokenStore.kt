package com.kiri.data.auth

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// 메모리에만 토큰을 두는 보관소. 여러 스레드에서 불려도 안전하다
internal class FakeTokenStore(initial: AuthTokens? = null) : TokenStore {
    private val state = MutableStateFlow(initial)
    private val saveCount = AtomicInteger()

    override val tokens: Flow<AuthTokens?> = state

    val saved: AuthTokens? get() = state.value
    val saves: Int get() = saveCount.get()

    override suspend fun save(tokens: AuthTokens) {
        saveCount.incrementAndGet()
        state.value = tokens
    }

    override suspend fun get(): AuthTokens? = state.value

    override suspend fun clear() {
        state.value = null
    }
}
