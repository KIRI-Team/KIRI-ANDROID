package com.kiri.feature.detail.model

import com.kiri.domain.detail.MissionPreview

data class MissionPreviewUiState(
    val loading: Boolean = true,
    val preview: MissionPreview? = null,
    val reviewsSelected: Boolean = false,
    val error: String? = null,
)

sealed interface MissionPreviewUiEvent {
    data object BackClicked : MissionPreviewUiEvent
    data object RetryClicked : MissionPreviewUiEvent
    data class ReviewsSelected(val selected: Boolean) : MissionPreviewUiEvent
}

sealed interface MissionPreviewUiEffect {
    data object NavigateBack : MissionPreviewUiEffect
}
