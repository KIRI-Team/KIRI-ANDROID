package com.kiri.data.preferences

import com.kiri.data.storage.*
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.place.PlaceCategory
import com.kiri.domain.preferences.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Serializable
private data class PreferenceValue(val categories: Set<String> = emptySet(), val environment: String? = null, val intensity: String? = null, val budget: String? = null, val duration: String? = null)
class LocalPreferenceRepository(private val store: LocalSettingsStore, private val home: HomeRepository) : PreferenceRepository {
    private suspend fun key() = "preferences:${home.load().currentUserId}"
    override suspend fun load(): UserPreferences {
        val encoded = store.read(key()) ?: return UserPreferences().also { save(it) }
        val value = Json { ignoreUnknownKeys = true }.decodeFromString<PreferenceValue>(encoded)
        return UserPreferences(value.categories.mapNotNull { name -> PlaceCategory.entries.find { it.name == name } }.take(3).toSet(),
            Environment.entries.find { it.name == value.environment } ?: Environment.ANY, Intensity.entries.find { it.name == value.intensity } ?: Intensity.MEDIUM, Budget.entries.find { it.name == value.budget } ?: Budget.ANY, ActivityDuration.entries.find { it.name == value.duration } ?: ActivityDuration.ANY)
    }
    override suspend fun save(preferences: UserPreferences) {
        require(preferences.valid) { "활동은 최대 3개까지 선택할 수 있어요." }
        store.write(key(), Json.encodeToString(PreferenceValue(preferences.categories.map { it.name }.toSet(), preferences.environment?.name, preferences.intensity?.name, preferences.budget?.name, preferences.duration?.name)))
    }
}
@Module
@InstallIn(SingletonComponent::class)
object PreferenceModule {
    @Provides @Singleton fun store(store: DataStoreLocalSettings): LocalSettingsStore = store
    @Provides @Singleton fun preferences(store: LocalSettingsStore, home: HomeRepository): PreferenceRepository = LocalPreferenceRepository(store, home)
}
