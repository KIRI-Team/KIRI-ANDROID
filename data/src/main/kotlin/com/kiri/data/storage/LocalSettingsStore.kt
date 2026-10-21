package com.kiri.data.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

interface LocalSettingsStore {
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String)
}
private val Context.kiriLocalSettings by preferencesDataStore("kiri_local_settings")
@Singleton
class DataStoreLocalSettings @Inject constructor(@ApplicationContext context: Context) : LocalSettingsStore {
    private val store = context.kiriLocalSettings
    override suspend fun read(key: String) = store.data.first()[stringPreferencesKey(key)]
    override suspend fun write(key: String, value: String) { store.edit { it[stringPreferencesKey(key)] = value } }
}
