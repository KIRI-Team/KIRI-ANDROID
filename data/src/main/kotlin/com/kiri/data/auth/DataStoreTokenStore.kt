package com.kiri.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// 같은 파일을 여는 DataStore 는 앱에 하나만 있어야 해서 파일 수준에서 만든다
// 로그인한 계정에 묶인 값(가족 ID)도 여기에 두어 clear() 로 함께 지운다
internal val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_tokens")

// 앱 전용 저장소(DataStore)에 토큰을 보관한다
@Singleton
internal class DataStoreTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) : TokenStore {

    private val dataStore = context.authDataStore

    override suspend fun save(tokens: AuthTokens) {
        dataStore.edit {
            it[ACCESS_TOKEN] = tokens.accessToken
            it[REFRESH_TOKEN] = tokens.refreshToken
        }
    }

    override val tokens: Flow<AuthTokens?> = dataStore.data.map { it.toTokens() }

    override suspend fun get(): AuthTokens? = tokens.first()

    private fun Preferences.toTokens(): AuthTokens? {
        val accessToken = this[ACCESS_TOKEN] ?: return null
        val refreshToken = this[REFRESH_TOKEN] ?: return null
        return AuthTokens(accessToken, refreshToken)
    }

    // 토큰과 함께 이 계정의 가족 ID(DataStoreFamilyIdStore)도 지운다
    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }
}
