package com.kiri.data.family

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.kiri.data.auth.authDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

// 내 가족 공간 ID 보관소. 가족 공간을 만들 때 받은 값이나 가족 ID 조회 API 로 받은 값을 기억한다 (resolve 참고)
internal interface FamilyIdStore {
    suspend fun save(familyId: Long)

    // 아직 기억한 값이 없으면 null
    suspend fun get(): Long?
}

// 로그인 토큰과 같은 저장소에 둔다. 로그아웃·탈퇴·세션 만료로 토큰을 지우면 가족 ID도 함께 지워져
// 다른 계정으로 로그인했을 때 이전 계정의 가족 ID가 남지 않는다
@Singleton
internal class DataStoreFamilyIdStore @Inject constructor(
    @ApplicationContext context: Context,
) : FamilyIdStore {

    private val dataStore = context.authDataStore

    override suspend fun save(familyId: Long) {
        dataStore.edit { it[FAMILY_ID] = familyId }
    }

    override suspend fun get(): Long? = dataStore.data.first()[FAMILY_ID]

    private companion object {
        val FAMILY_ID = longPreferencesKey("family_id")
    }
}
