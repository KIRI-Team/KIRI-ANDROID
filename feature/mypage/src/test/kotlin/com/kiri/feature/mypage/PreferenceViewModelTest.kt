package com.kiri.feature.mypage

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.preferences.*
import com.kiri.domain.place.PlaceCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PreferenceViewModelTest {
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun teardown() { Dispatchers.resetMain() }
    @Test fun preferenceLimitAndFailedSaveKeepDraftForRetryAndRestore() = runTest {
        var fail = true
        var stored = UserPreferences()
        val source = object : PreferenceRepository {
            override suspend fun load() = stored
            override suspend fun save(preferences: UserPreferences) { if (fail) error("network"); stored = preferences }
        }
        val handle = SavedStateHandle()
        val vm = PreferenceViewModel(source, handle)
        PlaceCategory.entries.take(4).forEach(vm::toggle)
        assertEquals(3, vm.uiState.value.value.categories.size)
        vm.change(vm.uiState.value.value.copy(environment = Environment.ANY, intensity = Intensity.LOW, budget = Budget.FREE, duration = ActivityDuration.SHORT))
        vm.save()
        assertNotNull(vm.uiState.value.error)
        val restored = PreferenceViewModel(source, handle)
        assertEquals(vm.uiState.value.value, restored.uiState.value.value)
        fail = false; restored.save()
        assertEquals(restored.uiState.value.value, stored)
        assertTrue(restored.uiState.value.saved)
    }
}
