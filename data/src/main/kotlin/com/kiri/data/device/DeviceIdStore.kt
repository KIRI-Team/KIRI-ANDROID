package com.kiri.data.device

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

// 서버가 이 기기를 구분하는 식별자. 앱을 설치한 뒤 처음 쓸 때 한 번 만들고 계속 같은 값을 쓴다
internal fun interface DeviceIdStore {
    suspend fun get(): String
}

// 로그아웃하면 비우는 로그인 토큰 저장소(authDataStore)와 따로 둬서, 다시 로그인해도 같은 기기로 등록된다
private val Context.pushDeviceDataStore: DataStore<Preferences> by preferencesDataStore(name = "push_device")

@Singleton
internal class DataStoreDeviceIdStore @Inject constructor(
    @ApplicationContext context: Context,
) : DeviceIdStore {

    private val dataStore = context.pushDeviceDataStore

    // edit 는 한 번에 하나씩 실행되므로 동시에 불려도 식별자는 하나만 만들어진다
    override suspend fun get(): String {
        dataStore.data.first()[DEVICE_ID]?.let { return it }
        val saved = dataStore.edit { if (it[DEVICE_ID] == null) it[DEVICE_ID] = UUID.randomUUID().toString() }
        // 바로 위 edit 에서 비어 있으면 채웠으므로 항상 값이 있다
        return checkNotNull(saved[DEVICE_ID])
    }

    private companion object {
        val DEVICE_ID = stringPreferencesKey("device_identifier")
    }
}
