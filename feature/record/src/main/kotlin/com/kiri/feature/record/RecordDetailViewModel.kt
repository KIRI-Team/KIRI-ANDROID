package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.record.*
import com.kiri.feature.detail.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class RecordDetailUiState(
    val collection: RecordCollection = RecordCollection(),
    val entry: CalendarEntry? = null,
    val detail: DetailUiState = DetailUiState(),
    val selectedPhotoId: String? = null,
    val editingMemoryId: String? = null,
    val memoryDraft: MemoryDraft = MemoryDraft(),
    val toast: String? = null,
) {
    val photos get() = collection.photos[entry?.id].orEmpty()
    val representativePhoto get() = photos.find { it.id == entry?.representativePhotoId }
    val memories get() = collection.memories.filter { it.entryId == entry?.id }
}

sealed interface RecordDetailEffect {
    data object Back : RecordDetailEffect
    data object Custom : RecordDetailEffect
    data object EditMemory : RecordDetailEffect
    data object ReturnToDetail : RecordDetailEffect
}

@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    private val repository: RecordRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val entryId = savedState.get<String>("entryId").orEmpty()
    private val state = MutableStateFlow(RecordDetailUiState(
        selectedPhotoId = savedState["selectedPhotoId"],
        editingMemoryId = savedState["editingMemoryId"],
        memoryDraft = MemoryDraft(savedState["editRating"] ?: 0, savedState.get<ArrayList<String>>("editPhotos").orEmpty(), savedState["editText"] ?: "", savedState["editRepeat"]),
    ))
    val uiState = state.asStateFlow()
    private val effects = Channel<RecordDetailEffect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.records.collect { collection ->
                state.update { it.copy(collection = collection, entry = collection.entries.find { entry -> entry.id == entryId }) }
            }
        }
        load()
    }

    fun load() = viewModelScope.launch {
        state.update { it.copy(detail = it.detail.copy(loading = true, error = null)) }
        try {
            val detail = repository.detail(entryId)
            val restored = savedState.get<ArrayList<String>>("recordConditionDraft")?.mapNotNull { name -> com.kiri.domain.detail.Condition.entries.find { it.name == name } }?.toSet()
            state.update { it.copy(detail = it.detail.copy(loading = false, detail = detail, draft = restored ?: detail.conditions)) }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { state.update { it.copy(detail = it.detail.copy(loading = false, error = "기록 정보를 불러오지 못했어요.")) } }
    }

    fun onDetailEvent(event: DetailUiEvent) {
        when (event) {
            DetailUiEvent.BackClicked -> effects.trySend(RecordDetailEffect.Back)
            DetailUiEvent.RetryClicked -> load()
            DetailUiEvent.CustomClicked -> {
                val entry = uiState.value.entry ?: return
                // TODO: 오늘 기준 통일 - 실제 RecordRepository 를 만들 때 기기 시계(LocalDate.now)와 저장소의 가족 날짜(family.date) 중 하나로 맞춘다
                if (entry.kind != CalendarEntryKind.SCHEDULE || entry.date != LocalDate.now(ZoneId.of("Asia/Seoul")) || uiState.value.detail.saving) return
                state.update { it.copy(detail = it.detail.copy(draft = it.detail.detail?.conditions.orEmpty(), error = null)) }
                effects.trySend(RecordDetailEffect.Custom)
            }
            is DetailUiEvent.ConditionToggled -> if (!uiState.value.detail.saving) {
                state.update {
                val draft = it.detail.draft
                it.copy(detail = it.detail.copy(draft = if (event.condition in draft) draft - event.condition else draft + event.condition))
                }
                savedState["recordConditionDraft"] = ArrayList(uiState.value.detail.draft.map { it.name })
            }
            DetailUiEvent.CustomCancelled -> if (!uiState.value.detail.saving) {
                savedState.remove<ArrayList<String>>("recordConditionDraft")
                effects.trySend(RecordDetailEffect.Back)
            }
            DetailUiEvent.ApplyClicked -> mutate {
                val detail = repository.applyConditions(entryId, uiState.value.detail.draft)
                savedState.remove<ArrayList<String>>("recordConditionDraft")
                state.update { it.copy(detail = it.detail.copy(detail = detail, draft = detail.conditions)) }
                effects.send(RecordDetailEffect.ReturnToDetail)
            }
            is DetailUiEvent.MemoriesSelected -> state.update { it.copy(detail = it.detail.copy(memoriesSelected = event.selected)) }
            DetailUiEvent.RecordClicked -> Unit
        }
    }

    fun beginPhotoSelection() {
        selectPhoto(uiState.value.entry?.representativePhotoId)
    }

    fun selectPhoto(id: String?) {
        if (id != null && uiState.value.photos.none { it.id == id }) return
        savedState["selectedPhotoId"] = id
        state.update { it.copy(selectedPhotoId = id) }
    }

    fun savePhoto() {
        val id = uiState.value.selectedPhotoId ?: return
        if (id == uiState.value.entry?.representativePhotoId) return
        mutate {
            repository.changeRepresentativePhoto(entryId, id)
            state.update { it.copy(toast = "사진 변경이 완료되었습니다!") }
            effects.send(RecordDetailEffect.ReturnToDetail)
        }
    }

    fun editMemory(id: String) {
        if (uiState.value.detail.saving) return
        val memory = uiState.value.memories.find { it.id == id && it.authorId == uiState.value.collection.currentUserId } ?: return
        savedState["editingMemoryId"] = id
        state.update { it.copy(editingMemoryId = id) }
        updateDraft(MemoryDraft(memory.rating, memory.photos.map { it.source }, memory.text, memory.repeatActivity))
        effects.trySend(RecordDetailEffect.EditMemory)
    }

    fun updateDraft(draft: MemoryDraft) {
        if (uiState.value.detail.saving) return
        val normalized = draft.copy(text = draft.text.take(500), rating = draft.rating.coerceIn(0, 5), photos = draft.photos.distinct())
        savedState["editText"] = normalized.text
        savedState["editRating"] = normalized.rating
        savedState["editPhotos"] = ArrayList(normalized.photos)
        savedState["editRepeat"] = normalized.repeatActivity
        state.update { it.copy(memoryDraft = normalized) }
    }

    fun saveMemory() {
        val memory = uiState.value.memories.find { it.id == uiState.value.editingMemoryId }
        val draft = uiState.value.memoryDraft
        if (!draft.canSubmit) return
        mutate {
            val photos = draft.photos.map { source -> memory?.photos?.find { it.source == source } ?: RecordPhoto(source, source) }
            if (memory == null) repository.addMemory(entryId, draft.text, photos, draft.rating, requireNotNull(draft.repeatActivity))
            else repository.updateMemory(memory.copy(text = draft.text, rating = draft.rating, repeatActivity = draft.repeatActivity, photos = photos))
            state.update { it.copy(toast = if (memory == null) "기록이 등록되었습니다!" else "기록이 수정되었습니다!") }
            effects.send(RecordDetailEffect.ReturnToDetail)
        }
    }

    fun prepareReview() {
        if (savedState.get<Boolean>("reviewPrepared") == true || uiState.value.entry == null) return
        val memory = uiState.value.memories.firstOrNull { it.authorId == uiState.value.collection.currentUserId }
        if (memory != null) {
            savedState["editingMemoryId"] = memory.id
            state.update { it.copy(editingMemoryId = memory.id) }
            updateDraft(MemoryDraft(memory.rating, memory.photos.map { it.source }, memory.text, memory.repeatActivity))
        }
        savedState["reviewPrepared"] = true
    }

    fun deleteBucket() = mutate {
        repository.deleteBucket(entryId)
        effects.send(RecordDetailEffect.Back)
    }

    fun deleteMemory(id: String) = mutate {
        repository.deleteMemory(id)
        state.update { it.copy(toast = "기록이 삭제되었습니다!") }
    }

    fun dismissToast() { state.update { it.copy(toast = null) } }

    private fun mutate(block: suspend () -> Unit) {
        if (uiState.value.detail.saving) return
        state.update { it.copy(detail = it.detail.copy(saving = true, error = null)) }
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { state.update { it.copy(detail = it.detail.copy(error = "변경하지 못했어요. 다시 시도해주세요.")) } }
            finally { state.update { it.copy(detail = it.detail.copy(saving = false)) } }
        }
    }
}
