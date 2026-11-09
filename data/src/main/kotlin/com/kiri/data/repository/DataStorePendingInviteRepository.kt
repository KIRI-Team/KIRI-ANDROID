package com.kiri.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kiri.domain.repository.PendingInviteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

// 로그인 토큰 저장소(authDataStore)와 따로 둔다. 링크를 누른 사람은 아직 로그인 전일 수 있고,
// 로그인 실패·세션 만료로 로그인 토큰을 지워도 초대 토큰은 가족에 참여할 때까지 남아 있어야 한다
private val Context.pendingInviteDataStore by preferencesDataStore(name = "pending_invite")

@Singleton
internal class DataStorePendingInviteRepository
internal constructor(private val dataStore: DataStore<Preferences>) : PendingInviteRepository {
    @Inject constructor(@ApplicationContext context: Context) : this(context.pendingInviteDataStore)

    override suspend fun save(token: String) {
        // 빈 값은 저장하지 않는다 (InviteLink.parseToken 이 이미 거르지만 한 번 더 막는다)
        if (token.isBlank()) return
        dataStore.edit { it[TOKEN] = token }
    }

    override suspend fun get(): String? = dataStore.data.first()[TOKEN]

    override suspend fun clear() {
        dataStore.edit { it.remove(TOKEN) }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("invite_token")
    }
}
