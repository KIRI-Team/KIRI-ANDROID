package com.kiri.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kiri.domain.home.FamilyHome
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Versioned JSON snapshots. A write must finish before repositories publish success. */
interface LocalStateStore {
    suspend fun read(key: String): String?

    suspend fun write(key: String, value: String)

    /** All snapshots become durable together, or none do. */
    suspend fun writeAtomically(values: Map<String, String>) {
        throw UnsupportedOperationException("Atomic snapshot writes are required")
    }
}

class MemoryLocalStateStore : LocalStateStore {
    private val mutex = Mutex()
    private val values = mutableMapOf<String, String>()

    override suspend fun read(key: String): String? = mutex.withLock { values[key] }

    override suspend fun write(key: String, value: String) {
        mutex.withLock { values[key] = value }
    }

    override suspend fun writeAtomically(values: Map<String, String>) {
        mutex.withLock { this.values.putAll(values) }
    }
}

private val Context.activityDataStore by preferencesDataStore(name = "local_activity_state")

@Singleton
internal class DataStoreLocalStateStore
internal constructor(private val dataStore: DataStore<Preferences>) : LocalStateStore {
    @Inject constructor(@ApplicationContext context: Context) : this(context.activityDataStore)

    override suspend fun read(key: String): String? =
        dataStore.data.first()[stringPreferencesKey(key)]

    override suspend fun write(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    override suspend fun writeAtomically(values: Map<String, String>) {
        dataStore.edit { preferences ->
            values.forEach { (key, value) -> preferences[stringPreferencesKey(key)] = value }
        }
    }
}

/**
 * Until the API supplies familyId, membership changes intentionally open a new namespace. Demo
 * sessions are separate; real accounts never share local family edits across logins.
 */
internal fun FamilyHome.localStorageScope(): String? {
    if (
        browsing ||
            currentUserId.isBlank() ||
            members.none { it.id == currentUserId && it.accessible }
    )
        return null
    val identity =
        if (!serverBacked) "demo:$familyName"
        else {
            val family =
                familyId
                    ?: members
                        .filter { it.accessible }
                        .map { it.id }
                        .sorted()
                        .joinToString("\u0000")
            "account:$currentUserId\u0000$family"
        }
    return MessageDigest.getInstance("SHA-256")
        .digest(identity.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocalStateModule {
    @Binds @Singleton abstract fun store(store: DataStoreLocalStateStore): LocalStateStore
}
