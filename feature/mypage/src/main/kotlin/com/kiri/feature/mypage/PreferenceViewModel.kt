package com.kiri.feature.mypage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.preferences.*
import com.kiri.domain.place.PlaceCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreferenceUiState(val loading: Boolean = true, val saving: Boolean = false, val value: UserPreferences = UserPreferences(), val error: String? = null, val saved: Boolean = false)
@HiltViewModel
class PreferenceViewModel @Inject constructor(private val repository: PreferenceRepository, private val handle: SavedStateHandle) : ViewModel() {
    private val state = MutableStateFlow(PreferenceUiState())
    val uiState = state.asStateFlow()
    init { reload() }
    fun reload() = viewModelScope.launch {
        state.update { it.copy(loading = true, error = null) }
        try {
            val loaded = repository.load()
            val value = if (handle.get<Boolean>("preferenceDraft") == true) UserPreferences(
                handle.get<ArrayList<String>>("preferenceCategories").orEmpty().mapNotNull { name -> PlaceCategory.entries.find { it.name == name } }.toSet(),
                Environment.entries.find { it.name == handle.get<String>("preferenceEnvironment") }, Intensity.entries.find { it.name == handle.get<String>("preferenceIntensity") },
                Budget.entries.find { it.name == handle.get<String>("preferenceBudget") }, ActivityDuration.entries.find { it.name == handle.get<String>("preferenceDuration") }) else loaded
            state.update { it.copy(loading = false, value = value) }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { state.update { it.copy(loading = false, error = "취향을 불러오지 못했어요. 다시 시도해주세요.") } }
    }
    fun toggle(category: PlaceCategory) {
        val current = state.value.value
        if (category !in current.categories && current.categories.size >= 3) { state.update { it.copy(error = "선호 활동은 최대 3개까지 선택할 수 있어요.") }; return }
        change(current.copy(categories = if (category in current.categories) current.categories - category else current.categories + category))
    }
    fun change(value: UserPreferences) {
        if (state.value.loading || state.value.saving) return
        state.update { it.copy(value = value, error = null, saved = false) }
        handle["preferenceDraft"] = true; handle["preferenceCategories"] = ArrayList(value.categories.map { it.name })
        handle["preferenceEnvironment"] = value.environment?.name; handle["preferenceIntensity"] = value.intensity?.name
        handle["preferenceBudget"] = value.budget?.name; handle["preferenceDuration"] = value.duration?.name
    }
    fun save() {
        if (!state.value.value.valid || state.value.loading || state.value.saving) return
        val value = state.value.value
        state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try { repository.save(value); handle["preferenceDraft"] = false; state.update { it.copy(saved = true) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { state.update { it.copy(error = "저장하지 못했어요. 선택은 유지했어요.") } }
            finally { state.update { it.copy(saving = false) } }
        }
    }
}
