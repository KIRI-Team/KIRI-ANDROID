package com.kiri.feature.detail.model

import com.kiri.domain.detail.MissionPreview

data class MissionPreviewUiState(
    val loading: Boolean = true,
    val preview: MissionPreview? = null,
    val reviewsSelected: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val previouslyCompleted: Boolean = false,
    val saving: Boolean = false,
    val canSave: Boolean = true,
) {
    val saveLabel get() = when {
        saving -> "저장 중"
        saved -> "저장됨"
        previouslyCompleted -> "다시 버킷에 저장"
        else -> "버킷에 저장"
    }
}

sealed interface MissionPreviewUiEvent {
    data object BackClicked : MissionPreviewUiEvent
    data object RetryClicked : MissionPreviewUiEvent
    data object SaveClicked : MissionPreviewUiEvent
    data class ReviewsSelected(val selected: Boolean) : MissionPreviewUiEvent
}

sealed interface MissionPreviewUiEffect {
    data object NavigateBack : MissionPreviewUiEffect
}
