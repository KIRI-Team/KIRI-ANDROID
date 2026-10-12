package com.kiri.feature.place.model

sealed interface PlaceUiEffect {
    data object NavigateBack : PlaceUiEffect
    data class OpenMission(val missionId: String) : PlaceUiEffect
}
