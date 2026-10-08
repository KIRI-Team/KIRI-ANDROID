package com.kiri.feature.detail.model

sealed interface DetailUiEffect {
    data object NavigateBack : DetailUiEffect
    data object OpenCustom : DetailUiEffect
}
