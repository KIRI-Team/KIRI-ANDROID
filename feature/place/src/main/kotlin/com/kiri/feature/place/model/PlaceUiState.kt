package com.kiri.feature.place.model

import com.kiri.domain.place.*

data class PlaceUiState(
    val query: PlaceQuery = PlaceQuery(), val items: List<PlaceMission> = emptyList(),
    val totalCount: Int = 0, val nextCursor: String? = null, val loading: Boolean = true,
    val loadingMore: Boolean = false, val error: String? = null, val pending: PlaceMission? = null,
    val saving: Boolean = false, val toast: Boolean = false,
    val savedItems: List<PlaceMission> = emptyList(), val showingSaved: Boolean = false,
)
