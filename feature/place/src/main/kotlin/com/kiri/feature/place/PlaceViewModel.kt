package com.kiri.feature.place

import com.kiri.feature.place.model.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kiri.domain.place.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@HiltViewModel
class PlaceViewModel @Inject constructor(private val repository: PlaceRepository, private val savedState: SavedStateHandle = SavedStateHandle()) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaceUiState(query = PlaceQuery(filters = restoreFilters())))
    val uiState = _uiState.asStateFlow()
    private val _effect = kotlinx.coroutines.channels.Channel<PlaceUiEffect>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var loadJob: Job? = null
    private var generation = 0
    init { reload() }
    fun onEvent(event: PlaceUiEvent) {
        when (event) {
            is PlaceUiEvent.FiltersApplied -> {
                savedState["filter_regions"] = ArrayList(event.filters.regions)
                savedState["filter_values"] = longArrayOf(event.filters.distanceKm?.toLong() ?: -1L,
                    event.filters.dateEpochDay ?: Long.MIN_VALUE, event.filters.minutesOfDay.toLong(),
                    event.filters.people.toLong(), event.filters.minPrice.toLong(), event.filters.maxPrice.toLong())
                _uiState.update { it.copy(query = it.query.copy(filters = event.filters, cursor = null)) }
                reload()
            }
            is PlaceUiEvent.MissionClicked -> _effect.trySend(PlaceUiEffect.OpenMission(event.missionId))
            is PlaceUiEvent.SearchChanged -> search(event.keyword)
            is PlaceUiEvent.CategorySelected -> category(event.category)
            is PlaceUiEvent.SaveClicked -> requestSave(event.mission)
            PlaceUiEvent.RetryClicked -> reload()
            PlaceUiEvent.MoreRequested -> loadMore()
            PlaceUiEvent.SaveConfirmed -> save()
            PlaceUiEvent.SaveDismissed -> dismissSave()
            PlaceUiEvent.NoticeDismissed -> dismissToast()
            PlaceUiEvent.SavedClicked -> showSaved()
            PlaceUiEvent.SavedBackClicked -> hideSaved()
            PlaceUiEvent.BackClicked -> _effect.trySend(PlaceUiEffect.NavigateBack)
        }
    }
    private fun restoreFilters(): PlaceFilters {
        val values = savedState.get<LongArray>("filter_values") ?: return PlaceFilters()
        return runCatching { PlaceFilters(savedState.get<ArrayList<String>>("filter_regions")?.toSet().orEmpty(),
            values[0].toInt().takeIf { it > 0 }, values[1].takeIf { it != Long.MIN_VALUE },
            values[2].toInt(), values[3].toInt(), values[4].toInt(), values[5].toInt()) }.getOrDefault(PlaceFilters())
    }
    private fun search(keyword: String) { _uiState.update { it.copy(query = it.query.copy(keyword = keyword, cursor = null)) }; reload(debounce = true) }
    private fun category(category: PlaceCategory?) { _uiState.update { it.copy(query = it.query.copy(category = category, cursor = null)) }; reload() }
    private fun reload(debounce: Boolean = false) {
        loadJob?.cancel()
        val version = ++generation
        val query = uiState.value.query.copy(cursor = null)
        _uiState.update { it.copy(loading = true, loadingMore = false, error = null, items = emptyList(), totalCount = 0, nextCursor = null) }
        loadJob = viewModelScope.launch {
            try {
                if (debounce) delay(300)
                val page = repository.load(query)
                if (version == generation) _uiState.update { current -> current.copy(loading = false,
                    items = page.items,
                    totalCount = page.totalCount, nextCursor = page.nextCursor) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (version == generation) _uiState.update { it.copy(loading = false, error = "활동을 불러오지 못했어요. 다시 시도해주세요.") } }
        }
    }
    private fun loadMore() {
        val current = uiState.value
        val cursor = current.nextCursor ?: return
        if (current.loading || current.loadingMore) return
        val version = generation
        _uiState.update { it.copy(loadingMore = true, error = null) }
        viewModelScope.launch {
            try {
                val page = repository.load(current.query.copy(cursor = cursor))
                if (version == generation) _uiState.update { it.copy(items = (it.items + page.items).distinctBy { mission -> mission.id }, totalCount = page.totalCount, nextCursor = page.nextCursor, loadingMore = false) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (version == generation) _uiState.update { it.copy(loadingMore = false, error = "다음 활동을 불러오지 못했어요.") } }
        }
    }
    private fun requestSave(mission: PlaceMission) { if (!mission.saved && !uiState.value.saving) _uiState.update { it.copy(pending = mission, error = null) } }
    private fun dismissSave() { if (!uiState.value.saving) _uiState.update { it.copy(pending = null, error = null) } }
    private fun save() {
        val mission = uiState.value.pending ?: return
        if (uiState.value.saving) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                repository.save(mission.id)
                _uiState.update { it.copy(saving = false, pending = null, toast = true,
                    items = it.items.map { item -> if (item.id == mission.id) item.copy(saved = true) else item },
                    savedItems = (listOf(mission.copy(saved = true)) + it.savedItems.filterNot { item -> item.id == mission.id })) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _uiState.update { it.copy(saving = false, error = "저장하지 못했어요. 다시 시도해주세요.") } }
        }
    }
    private fun dismissToast() { _uiState.update { it.copy(toast = false) } }
    private fun showSaved() {
        _uiState.update { it.copy(showingSaved = true, toast = false, error = null) }
        viewModelScope.launch {
            try { val saved = repository.saved(); _uiState.update { it.copy(savedItems = saved) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _uiState.update { it.copy(error = "저장한 활동을 불러오지 못했어요.") } }
        }
    }
    private fun hideSaved() { _uiState.update { it.copy(showingSaved = false, error = null) } }

    /** Re-read repository state after a review or save on another screen. */
    fun refreshSaves() {
        if (uiState.value.saving) return
        reload()
        if (uiState.value.showingSaved) showSaved()
    }
}
