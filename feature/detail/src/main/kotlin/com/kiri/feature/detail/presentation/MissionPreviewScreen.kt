package com.kiri.feature.detail.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.detail.MissionPreviewViewModel
import com.kiri.feature.detail.model.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.feature.detail.model.DetailUiState
import com.kiri.feature.detail.model.DetailUiEvent

@Composable
fun MissionPreviewRoute(onBack: () -> Unit, viewModel: MissionPreviewViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentBack by rememberUpdatedState(onBack)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.effect.collect { currentBack() }
        }
    }
    MissionPreviewScreen(state,
        { viewModel.onEvent(MissionPreviewUiEvent.BackClicked) },
        { viewModel.onEvent(MissionPreviewUiEvent.RetryClicked) },
        { viewModel.onEvent(MissionPreviewUiEvent.ReviewsSelected(it)) },
    )
}

@Composable
fun MissionPreviewScreen(state: MissionPreviewUiState, onBack: () -> Unit, onRetry: () -> Unit, onSelectReviews: (Boolean) -> Unit) {
    MissionDetailScreen(
        DetailUiState(loading = state.loading, detail = state.preview?.detail, error = state.error),
        onEvent = { event ->
            when (event) {
                DetailUiEvent.BackClicked -> onBack()
                DetailUiEvent.RetryClicked -> onRetry()
                else -> Unit
            }
        },
        discovery = true, reviews = state.preview?.reviews.orEmpty(), reviewsSelected = state.reviewsSelected,
        onSelectReviews = onSelectReviews, curated = state.preview?.curated == true,
    )
}
