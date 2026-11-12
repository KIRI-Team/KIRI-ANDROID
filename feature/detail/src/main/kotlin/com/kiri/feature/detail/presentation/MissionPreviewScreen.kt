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
fun MissionPreviewRoute(onBack: () -> Unit, viewModel: MissionPreviewViewModel = hiltViewModel(), showSaveAction: Boolean = true) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentBack by rememberUpdatedState(onBack)
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.retry()
            viewModel.effect.collect { effect ->
                when (effect) {
                    com.kiri.feature.detail.model.MissionPreviewUiEffect.NavigateBack -> currentBack()
                    // TODO: 제한 안내 UI 가 정해지면 바꾼다
                    is com.kiri.feature.detail.model.MissionPreviewUiEffect.ShowMessage ->
                        android.widget.Toast.makeText(context, effect.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    MissionPreviewScreen(state,
        { viewModel.onEvent(MissionPreviewUiEvent.BackClicked) },
        { viewModel.onEvent(MissionPreviewUiEvent.RetryClicked) },
        { viewModel.onEvent(MissionPreviewUiEvent.ReviewsSelected(it)) },
        { viewModel.onEvent(MissionPreviewUiEvent.SaveClicked) },
        showSaveAction = showSaveAction,
    )
}

@Composable
fun MissionPreviewScreen(state: MissionPreviewUiState, onBack: () -> Unit, onRetry: () -> Unit, onSelectReviews: (Boolean) -> Unit, onSave: () -> Unit = {}, showSaveAction: Boolean = true) {
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
        discoverySaveLabel = state.saveLabel.takeIf { showSaveAction },
        discoverySaveEnabled = state.canSave && !state.saved && !state.saving,
        onDiscoverySave = onSave,
    )
}
