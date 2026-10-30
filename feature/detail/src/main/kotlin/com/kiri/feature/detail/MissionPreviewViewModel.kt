package com.kiri.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.detail.DetailRepository
import com.kiri.feature.detail.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MissionPreviewViewModel @Inject constructor(
    private val repository: DetailRepository,
    private val savedState: SavedStateHandle,
    private val buckets: com.kiri.domain.record.BucketRepository? = null,
) : ViewModel() {
    private val missionId: String = savedState["missionId"] ?: ""
    private val _uiState = MutableStateFlow(MissionPreviewUiState(reviewsSelected = savedState["reviewsSelected"] ?: false))
    val uiState = _uiState.asStateFlow()
    private val _effect = Channel<MissionPreviewUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var loading = false
    init { retry() }

    fun onEvent(event: MissionPreviewUiEvent) {
        when (event) {
            MissionPreviewUiEvent.BackClicked -> _effect.trySend(MissionPreviewUiEffect.NavigateBack)
            MissionPreviewUiEvent.RetryClicked -> retry()
            MissionPreviewUiEvent.SaveClicked -> save()
            is MissionPreviewUiEvent.ReviewsSelected -> selectReviews(event.selected)
        }
    }

    fun selectReviews(selected: Boolean) {
        savedState["reviewsSelected"] = selected
        _uiState.update { it.copy(reviewsSelected = selected) }
    }

    fun retry() {
        if (loading || uiState.value.saving) return
        loading = true
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val preview = repository.preview(missionId)
                val owned = buckets?.mine().orEmpty().filter { it.missionId == missionId }
                _uiState.update { it.copy(loading = false, preview = preview,
                    saved = owned.any { bucket -> !bucket.completed },
                    previouslyCompleted = owned.any { bucket -> bucket.completed }) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _uiState.update { it.copy(loading = false, error = "미션 정보를 불러오지 못했어요.") } }
            finally { loading = false }
        }
    }

    private fun save() {
        val current = uiState.value
        if (loading || current.saving || current.saved || current.preview == null) return
        val repository = buckets ?: return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                repository.save(missionId)
                _uiState.update { it.copy(saving = false, saved = true) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _uiState.update { it.copy(saving = false, error = "저장하지 못했어요. 다시 시도해주세요.") }
            }
        }
    }
}
