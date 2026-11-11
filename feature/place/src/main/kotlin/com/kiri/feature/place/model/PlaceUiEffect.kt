package com.kiri.feature.place.model

sealed interface PlaceUiEffect {
    data object NavigateBack : PlaceUiEffect
    data class OpenMission(val missionId: String) : PlaceUiEffect

    // 토스트로 알린다 (둘러보기 중 가족 기능 제한 등)
    data class ShowMessage(val message: String) : PlaceUiEffect
}
