package com.kiri.feature.detail

import com.kiri.feature.detail.model.*

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.detail.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: DetailRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val missionId: String = savedState["missionId"] ?: ""
    private val _uiState = MutableStateFlow(DetailUiState(
        memoriesSelected = savedState["memoriesSelected"] ?: (savedState.get<String>("memories") == "true"),
        memoryDraft = MemoryDraft(
            rating = savedState["memoryRating"] ?: 0,
            photos = savedState.get<ArrayList<String>>("memoryPhotos").orEmpty(),
            text = savedState["memoryText"] ?: "",
            repeatActivity = savedState["memoryRepeat"],
        ),
    ))
    val uiState = _uiState.asStateFlow()
    private val _effect = Channel<DetailUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var loading = false

    init { load() }

    fun onEvent(event: DetailUiEvent) {
        when (event) {
            DetailUiEvent.RetryClicked -> load()
            DetailUiEvent.BackClicked -> _effect.trySend(DetailUiEffect.NavigateBack)
            DetailUiEvent.CustomClicked -> beginCustom()
            is DetailUiEvent.ConditionToggled -> toggle(event.condition)
            DetailUiEvent.CustomCancelled -> cancelCustom()
            DetailUiEvent.ApplyClicked -> apply()
            is DetailUiEvent.MemoriesSelected -> selectMemories(event.selected)
            DetailUiEvent.RecordClicked -> {
                val detail = uiState.value.detail
                if (detail != null && (detail.completed || detail.date?.let { !it.isAfter(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"))) } == true)) {
                    uiState.value.submittedMemory?.let(::updateMemoryDraft)
                    _effect.trySend(DetailUiEffect.OpenRecord)
                }
            }
        }
    }

    fun selectMemories(selected: Boolean) {
        savedState["memoriesSelected"] = selected
        _uiState.update { it.copy(memoriesSelected = selected) }
    }

    fun updateMemoryDraft(draft: MemoryDraft) {
        val normalized = draft.copy(rating = draft.rating.coerceIn(0, 5), text = draft.text.take(500), photos = draft.photos.distinct())
        savedState["memoryRating"] = normalized.rating
        savedState["memoryText"] = normalized.text
        savedState["memoryPhotos"] = ArrayList(normalized.photos)
        savedState["memoryRepeat"] = normalized.repeatActivity
        _uiState.update { it.copy(memoryDraft = normalized) }
    }

    fun submitMemoryPreview(): Boolean {
        val draft = uiState.value.memoryDraft
        if (!draft.canSubmit) return false
        _uiState.update { it.copy(submittedMemory = draft.copy(text = draft.text.trim()), memoriesSelected = true) }
        savedState["memoriesSelected"] = true
        return true
    }

    private fun load() {
        if (loading || uiState.value.saving) return
        loading = true
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val detail = repository.load(missionId)
                val restored = savedState.get<ArrayList<String>>("conditionDraft")?.mapNotNull { name ->
                    Condition.entries.firstOrNull { it.name == name }
                }?.toSet()
                _uiState.update { it.copy(loading = false, detail = detail, draft = restored ?: detail.conditions) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _uiState.update { it.copy(loading = false, error = "미션 정보를 불러오지 못했어요.") } }
            finally { loading = false }
        }
    }

    private fun beginCustom() {
        if (uiState.value.detail?.completed != false || uiState.value.saving) return
        setDraft(uiState.value.detail!!.conditions)
        _uiState.update { it.copy(error = null) }
        _effect.trySend(DetailUiEffect.OpenCustom)
    }

    private fun toggle(condition: Condition) {
        if (uiState.value.saving || uiState.value.detail?.completed != false) return
        val draft = uiState.value.draft
        setDraft(if (condition in draft) draft - condition else draft + condition)
        _uiState.update { it.copy(error = null) }
    }

    private fun cancelCustom() {
        if (!uiState.value.saving) {
            savedState.remove<ArrayList<String>>("conditionDraft")
            _uiState.update { it.copy(draft = it.detail?.conditions.orEmpty(), error = null) }
            _effect.trySend(DetailUiEffect.NavigateBack)
        }
    }


    private fun apply() {
        val current = uiState.value
        if (current.saving || current.detail?.completed != false) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val detail = repository.applyConditions(missionId, current.draft)
                savedState.remove<ArrayList<String>>("conditionDraft")
                _uiState.update { it.copy(detail = detail, draft = detail.conditions, saving = false) }
                _effect.trySend(DetailUiEffect.NavigateBack)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _uiState.update { it.copy(saving = false, error = "적용하지 못했어요. 선택은 유지됐으니 다시 시도해주세요.") } }
        }
    }

    private fun setDraft(draft: Set<Condition>) {
        savedState["conditionDraft"] = ArrayList(draft.map { it.name })
        _uiState.update { it.copy(draft = draft) }
    }
}
