package com.kiri.data.preferences

import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.preferences.LocalPreferenceRepository
import com.kiri.data.storage.LocalSettingsStore
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.place.PlaceCategory
import com.kiri.domain.preferences.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class PreferenceStorageTest {
    private class Store : LocalSettingsStore {
        val values = mutableMapOf<String, String>()
        override suspend fun read(key: String) = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
    }
    private val clock = Clock.fixed(Instant.parse("2026-10-19T03:00:00Z"), ZoneOffset.UTC)
    @Test fun preferencesPersistAcrossInstancesAndRemainScopedToTheUser() = runTest {
        val store = Store(); val source = FakeHomeRepository(clock, latencyMillis = 0)
        var user = "me"
        val home = object : HomeRepository by source { override suspend fun load() = source.load().copy(currentUserId = user) }
        val value = UserPreferences(setOf(PlaceCategory.CULTURE, PlaceCategory.OUTDOOR), Environment.ANY, Intensity.LOW, Budget.FREE, ActivityDuration.MEDIUM)
        LocalPreferenceRepository(store, home).save(value)
        assertEquals(value, LocalPreferenceRepository(store, home).load())
        assertTrue(runCatching { LocalPreferenceRepository(store, home).save(value.copy(categories = PlaceCategory.entries.take(4).toSet())) }.isFailure)
        user = "mother"
        assertEquals(UserPreferences(), LocalPreferenceRepository(store, home).load())
    }
    @Test fun unconfiguredDefaultsArePersistedWithoutRequiringACategory() = runTest {
        val store = Store()
        val home = FakeHomeRepository(clock, latencyMillis = 0)
        assertEquals(UserPreferences(), LocalPreferenceRepository(store, home).load())
        assertTrue(store.values.containsKey("preferences:me"))
        assertEquals(UserPreferences(), LocalPreferenceRepository(store, home).load())
    }
}
