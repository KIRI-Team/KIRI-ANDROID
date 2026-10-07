package com.kiri.feature.detail.presentation

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
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val detail: MissionDetail? = null,
    val draft: Set<Condition> = emptySet(),
    val saving: Boolean = false,
    val error: String? = null,
    val applied: Boolean = false,
) {
    val nearby: List<NearbyPlace> get() = detail?.let { rankNearby(it.nearby, it.conditions) }.orEmpty()
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: DetailRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val missionId: String = savedState["missionId"] ?: ""
    private val mutableState = MutableStateFlow(DetailUiState())
    val state = mutableState.asStateFlow()
    private var loading = false

    init { load() }

    fun load() {
        if (loading || state.value.saving) return
        loading = true
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val detail = repository.load(missionId)
                val restored = savedState.get<ArrayList<String>>("conditionDraft")?.mapNotNull { name ->
                    Condition.entries.firstOrNull { it.name == name }
                }?.toSet()
                mutableState.update { it.copy(loading = false, detail = detail, draft = restored ?: detail.conditions) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.update { it.copy(loading = false, error = "미션 정보를 불러오지 못했어요.") } }
            finally { loading = false }
        }
    }

    fun beginCustom() {
        if (state.value.detail?.completed != false || state.value.saving) return
        setDraft(state.value.detail!!.conditions)
        mutableState.update { it.copy(error = null, applied = false) }
    }

    fun toggle(condition: Condition) {
        if (state.value.saving || state.value.detail?.completed != false) return
        val draft = state.value.draft
        setDraft(if (condition in draft) draft - condition else draft + condition)
        mutableState.update { it.copy(error = null) }
    }

    fun cancelCustom() {
        if (!state.value.saving) {
            savedState.remove<ArrayList<String>>("conditionDraft")
            mutableState.update { it.copy(draft = it.detail?.conditions.orEmpty(), error = null) }
        }
    }

    fun consumeApplied() { mutableState.update { it.copy(applied = false) } }

    fun apply() {
        val current = state.value
        if (current.saving || current.detail?.completed != false) return
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val detail = repository.applyConditions(missionId, current.draft)
                savedState.remove<ArrayList<String>>("conditionDraft")
                mutableState.update { it.copy(detail = detail, draft = detail.conditions, saving = false, applied = true) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.update { it.copy(saving = false, error = "적용하지 못했어요. 선택은 유지됐으니 다시 시도해주세요.") } }
        }
    }

    private fun setDraft(draft: Set<Condition>) {
        savedState["conditionDraft"] = ArrayList(draft.map { it.name })
        mutableState.update { it.copy(draft = draft) }
    }
}
