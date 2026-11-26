package com.kiri.feature.place.model

import com.kiri.domain.place.model.*
import com.kiri.domain.place.repository.*

sealed interface PlaceUiEvent {
    data class FiltersApplied(val filters: PlaceFilters) : PlaceUiEvent
    data class SearchChanged(val keyword: String) : PlaceUiEvent
    data class CategorySelected(val category: PlaceCategory?) : PlaceUiEvent
    data class SaveClicked(val mission: PlaceMission) : PlaceUiEvent
    data class MissionClicked(val missionId: String, val kiriPickId: Long? = null) : PlaceUiEvent
    data object RetryClicked : PlaceUiEvent
    data object MoreRequested : PlaceUiEvent
    data object SaveConfirmed : PlaceUiEvent
    data object SaveDismissed : PlaceUiEvent
    data object NoticeDismissed : PlaceUiEvent
    data object SavedClicked : PlaceUiEvent
    data object SavedBackClicked : PlaceUiEvent
    data object BackClicked : PlaceUiEvent
}
