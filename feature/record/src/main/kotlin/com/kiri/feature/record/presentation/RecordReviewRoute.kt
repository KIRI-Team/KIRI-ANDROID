package com.kiri.feature.record.presentation

import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.record.RecordDetailViewModel

/** New and existing reviews share the same validated editor and repository. */
@Composable
fun RecordReviewRoute(onBack: () -> Unit, viewModel: RecordDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.entry?.id) { viewModel.prepareReview() }
    RecordDetailEffects(viewModel, onBack = onBack, returnToDetail = true)
    MemoryEditorScreen(state.memoryDraft, state.collection.savers[state.collection.currentUserId] ?: "나",
        viewModel::updateDraft, onBack, viewModel::saveMemory, editing = state.editingMemoryId != null,
        saving = state.detail.loading || state.detail.saving, error = state.detail.error)
}
