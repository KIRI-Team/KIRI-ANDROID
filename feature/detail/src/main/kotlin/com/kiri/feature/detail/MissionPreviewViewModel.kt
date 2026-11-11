package com.kiri.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.detail.DetailRepository
import com.kiri.domain.detail.DetailSuggestion
import com.kiri.domain.detail.MissionDetail
import com.kiri.domain.detail.MissionPreview
import com.kiri.domain.model.mission.MissionBucketState
import com.kiri.domain.model.mission.MissionInfo
import com.kiri.domain.repository.MissionInfoRepository
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
import com.kiri.domain.home.isBrowsing

@HiltViewModel
class MissionPreviewViewModel @Inject constructor(
    private val repository: DetailRepository,
    private val savedState: SavedStateHandle,
    private val buckets: com.kiri.domain.record.BucketRepository? = null,
    private val serverMissions: MissionInfoRepository? = null,
    // 둘러보기 사용자는 버킷에 저장할 수 없다 (가족 정보로 확인). 없으면 확인하지 않는다
    private val home: com.kiri.domain.home.HomeRepository? = null,
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
                val serverId = missionId.toLongOrNull()
                if (serverId != null && serverMissions != null) {
                    val mission = serverMissions.getMission(serverId).getOrThrow()
                    _uiState.update { it.copy(
                        loading = false, preview = mission.toPreview(), canSave = true,
                        saved = mission.bucketState == MissionBucketState.SAVED,
                        previouslyCompleted = mission.bucketState == MissionBucketState.COMPLETED_ONLY,
                    ) }
                    return@launch
                }
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
        if (loading || current.saving || current.saved || !current.canSave || current.preview == null) return
        val serverId = missionId.toLongOrNull()
        val bucketRepository = buckets
        if (serverId == null && bucketRepository == null) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            // 둘러보기 중이면 저장하지 않고 안내한다
            if (home != null && home.isBrowsing()) {
                _uiState.update { it.copy(saving = false) }
                _effect.trySend(MissionPreviewUiEffect.ShowMessage(com.kiri.domain.home.BROWSING_BLOCKED_MESSAGE))
                return@launch
            }
            try {
                if (serverId != null && serverMissions != null) serverMissions.saveBucket(serverId).getOrThrow()
                else requireNotNull(bucketRepository).save(missionId)
                _uiState.update { it.copy(saving = false, saved = true) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _uiState.update { it.copy(saving = false, error = "저장하지 못했어요. 다시 시도해주세요.") }
            }
        }
    }
}

private fun MissionInfo.toPreview() = MissionPreview(
    MissionDetail(
        id = id.toString(), title = title, familyName = "",
        date = null, startTime = null, place = place?.name, expectedSteps = null,
        durationMinutes = durationMinMinutes, participants = emptyList(), completed = false,
        activities = steps.map { DetailSuggestion(it.title, it.description.orEmpty()) },
        essentials = prepareItems.map { DetailSuggestion(it.title, it.description.orEmpty()) },
        nearby = emptyList(), walkingLevel = walkingLevel?.label, durationMaxMinutes = durationMaxMinutes,
    ),
)
