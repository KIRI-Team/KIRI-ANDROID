package com.kiri.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.mission.model.DetailSuggestion
import com.kiri.domain.mission.model.MissionDetail
import com.kiri.domain.mission.model.MissionPreview
import com.kiri.domain.mission.model.MissionBucketState
import com.kiri.domain.mission.model.MissionInfo
import com.kiri.domain.event.repository.AppEventRepository
import com.kiri.domain.mission.repository.MissionInfoRepository
import com.kiri.domain.mission.repository.MissionNearbyRepository
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
import com.kiri.domain.family.model.isBrowsing

@HiltViewModel
class MissionPreviewViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val serverMissions: MissionInfoRepository,
    // 둘러보기 사용자는 버킷에 저장할 수 없다 (가족 정보로 확인). 없으면 확인하지 않는다
    private val home: com.kiri.domain.home.repository.HomeRepository? = null,
    private val appEvents: AppEventRepository? = null,
    private val missionNearby: MissionNearbyRepository? = null,
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
                // 서버 미션 id 가 아니면 보여줄 미션이 없다
                val serverId = requireNotNull(missionId.toLongOrNull())
                val mission = serverMissions.getMission(serverId).getOrThrow()
                _uiState.update { it.copy(
                    loading = false, preview = mission.toPreview(), canSave = true,
                    saved = mission.bucketState == MissionBucketState.SAVED,
                    previouslyCompleted = mission.bucketState == MissionBucketState.COMPLETED_ONLY,
                ) }
                recordDetailView(serverId)
                val essentials = serverMissions.getMissionEssentials(serverId)
                essentials.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                _uiState.update { state ->
                    state.copy(essentialsFailed = essentials.isFailure, preview = state.preview?.let { preview ->
                        preview.copy(detail = preview.detail.copy(essentials = essentials.getOrNull().orEmpty().map {
                            DetailSuggestion(it.title, it.description.orEmpty())
                        }))
                    })
                }
                val nearby = missionNearby?.getMissionNearby(serverId)
                nearby?.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                _uiState.update { state ->
                    state.copy(nearbyFailed = nearby?.isFailure == true, preview = state.preview?.let { preview ->
                        preview.copy(detail = preview.detail.copy(nearby = nearby?.getOrNull().orEmpty()))
                    })
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _uiState.update { it.copy(loading = false, error = "미션 정보를 불러오지 못했어요.") } }
            finally { loading = false }
        }
    }

    private fun recordDetailView(missionId: Long) {
        val events = appEvents ?: return
        if (savedState.get<Boolean>("detailViewRecorded") == true) return
        savedState["detailViewRecorded"] = true
        viewModelScope.launch { events.recordMissionDetailView(missionId) }
    }

    private fun save() {
        val current = uiState.value
        if (loading || current.saving || current.saved || !current.canSave || current.preview == null) return
        val serverId = missionId.toLongOrNull() ?: return
        val kiriPickId = savedState.get<String>("kiriPickId")?.toLongOrNull()
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            // 둘러보기 중이면 저장하지 않고 안내한다
            if (home != null && home.isBrowsing()) {
                _uiState.update { it.copy(saving = false) }
                _effect.trySend(MissionPreviewUiEffect.ShowMessage(com.kiri.domain.family.model.BROWSING_BLOCKED_MESSAGE))
                return@launch
            }
            try {
                val result = if (kiriPickId == null) serverMissions.saveBucket(serverId)
                else serverMissions.saveBucket(serverId, kiriPickId)
                result.getOrThrow()
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
        essentials = emptyList(),
        nearby = emptyList(), walkingLevel = walkingLevel?.label, durationMaxMinutes = durationMaxMinutes,
    ),
)
