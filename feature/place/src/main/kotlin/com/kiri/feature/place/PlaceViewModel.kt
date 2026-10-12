package com.kiri.feature.place

import com.kiri.feature.place.model.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.place.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@HiltViewModel
class PlaceViewModel @Inject constructor(private val repository: PlaceRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaceUiState())
    val uiState = _uiState.asStateFlow()
    private val _effect = kotlinx.coroutines.channels.Channel<PlaceUiEffect>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var loadJob: Job? = null
    private var generation = 0
    init { reload() }
    fun onEvent(event: PlaceUiEvent) {
        when (event) {
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
                    items = page.items.map { mission -> if (current.savedItems.any { it.id == mission.id }) mission.copy(saved = true) else mission },
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
                if (version == generation) _uiState.update { it.copy(items = (it.items + page.items.map { mission -> if (it.savedItems.any { saved -> saved.id == mission.id }) mission.copy(saved = true) else mission }).distinctBy { mission -> mission.id }, totalCount = page.totalCount, nextCursor = page.nextCursor, loadingMore = false) }
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
                    savedItems = (it.savedItems + mission.copy(saved = true)).distinctBy { item -> item.id }) }
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
}
