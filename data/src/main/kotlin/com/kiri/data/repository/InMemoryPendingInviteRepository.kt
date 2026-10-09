package com.kiri.data.repository

import com.kiri.domain.repository.PendingInviteRepository
import javax.inject.Inject
import javax.inject.Singleton

// 앱이 실행되는 동안만 보관한다
@Singleton
class InMemoryPendingInviteRepository @Inject constructor() : PendingInviteRepository {
    private var code: String? = null

    override fun save(code: String) = synchronized(this) {
        this.code = code
    }

    override fun consume(): String? = synchronized(this) {
        code.also { code = null }
    }
}
