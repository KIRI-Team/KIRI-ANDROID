package com.kiri.feature.record.navigation

import android.net.Uri
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import com.kiri.feature.detail.presentation.DetailScreen
import com.kiri.feature.record.presentation.RecordRoute
import com.kiri.feature.record.presentation.*
import com.kiri.feature.record.RecordDetailViewModel
import com.kiri.feature.detail.presentation.ConditionCustomScreen
import com.kiri.feature.detail.model.DetailUiEvent

private const val GRAPH_ROUTE = "record_graph"
private const val START_ROUTE = "record"
private const val DETAIL_ROUTE = "record_graph/detail"

/** Registers this feature in the main tab back stack. */
fun NavGraphBuilder.recordNavHost(navController: NavHostController, onCreateSchedule: (String) -> Unit = {}, onManageSchedule: (String) -> Unit = {}, scheduleDestinations: NavGraphBuilder.() -> Unit = {}) {
    navigation(startDestination = START_ROUTE, route = GRAPH_ROUTE) {
        scheduleDestinations()
        composable(START_ROUTE) {
            RecordRoute(
                onScheduleClick = { navController.navigate("$DETAIL_ROUTE/${Uri.encode(it)}") { launchSingleTop = true } },
                onBucketClick = { navController.navigate("$DETAIL_ROUTE/${Uri.encode(it)}") { launchSingleTop = true } },
                onEntryClick = { navController.navigate("$DETAIL_ROUTE/${Uri.encode(it)}") { launchSingleTop = true } },
            )
        }
        composable(DETAIL_ROUTE) {
            DetailScreen(title = "기록 상세", onBackClick = navController::popBackStack)
        }
        composable("$DETAIL_ROUTE/{entryId}") { entry ->
            val viewModel: RecordDetailViewModel = hiltViewModel(entry)
            val id = Uri.encode(entry.arguments?.getString("entryId").orEmpty())
            RecordDetailRoute(viewModel, onBack = { navController.popBackStack() },
                onCustom = { navController.navigate("$DETAIL_ROUTE/$id/custom") { launchSingleTop = true } },
                onPhoto = { navController.navigate("$DETAIL_ROUTE/$id/photos") { launchSingleTop = true } },
                onEdit = { navController.navigate("$DETAIL_ROUTE/$id/edit") { launchSingleTop = true } },
                onCreateSchedule = onCreateSchedule, onManageSchedule = onManageSchedule)
        }
        composable("$DETAIL_ROUTE/{entryId}/custom") { entry ->
            val parent = remember(entry) { navController.getBackStackEntry("$DETAIL_ROUTE/{entryId}") }
            val viewModel: RecordDetailViewModel = hiltViewModel(parent)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            RecordDetailEffects(viewModel, onBack = { navController.popBackStack() }, returnToDetail = true)
            ConditionCustomScreen(state.detail, viewModel::onDetailEvent, Modifier.padding(bottom = 110.dp))
        }
        dialog("$DETAIL_ROUTE/{entryId}/photos", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { entry ->
            val parent = remember(entry) { navController.getBackStackEntry("$DETAIL_ROUTE/{entryId}") }
            val viewModel: RecordDetailViewModel = hiltViewModel(parent)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            RecordDetailEffects(viewModel, onBack = { navController.popBackStack() }, returnToDetail = true)
            RecordPhotoSelectionScreen(state, onBack = { navController.popBackStack() }, viewModel::selectPhoto, viewModel::savePhoto)
        }
        composable("$DETAIL_ROUTE/{entryId}/edit") { entry ->
            val parent = remember(entry) { navController.getBackStackEntry("$DETAIL_ROUTE/{entryId}") }
            val viewModel: RecordDetailViewModel = hiltViewModel(parent)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            RecordDetailEffects(viewModel, onBack = { navController.popBackStack() }, returnToDetail = true)
            MemoryEditorScreen(state.memoryDraft, state.collection.memories.find { it.id == state.editingMemoryId }?.authorName.orEmpty(),
                onChange = viewModel::updateDraft, onBack = { navController.popBackStack() }, onSubmit = viewModel::saveMemory,
                editing = true, saving = state.detail.saving, error = state.detail.error)
        }
    }
}
