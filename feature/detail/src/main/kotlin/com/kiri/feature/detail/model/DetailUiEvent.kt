package com.kiri.feature.detail.model

import com.kiri.domain.detail.Condition

sealed interface DetailUiEvent {
    data object RetryClicked : DetailUiEvent
    data object BackClicked : DetailUiEvent
    data object CustomClicked : DetailUiEvent
    data class ConditionToggled(val condition: Condition) : DetailUiEvent
    data object CustomCancelled : DetailUiEvent
    data object ApplyClicked : DetailUiEvent
}
