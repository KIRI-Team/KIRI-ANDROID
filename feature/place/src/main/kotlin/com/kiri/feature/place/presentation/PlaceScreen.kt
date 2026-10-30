package com.kiri.feature.place.presentation

import com.kiri.feature.place.PlaceViewModel
import com.kiri.feature.place.model.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.domain.place.*
import com.kiri.feature.place.R
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.*
import kotlinx.coroutines.delay

@Composable
fun PlaceRoute(onBack: () -> Unit = {}, viewModel: PlaceViewModel = hiltViewModel(), onMission: (String) -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentBack by rememberUpdatedState(onBack)
    val currentMission by rememberUpdatedState(onMission)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshSaves()
            viewModel.effect.collect { effect ->
                when (effect) {
                    PlaceUiEffect.NavigateBack -> currentBack()
                    is PlaceUiEffect.OpenMission -> currentMission(effect.missionId)
                }
            }
        }
    }
    PlaceScreen(state, viewModel::onEvent)
}

@Composable
fun PlaceScreen(state: PlaceUiState, onEvent: (PlaceUiEvent) -> Unit) {
    var showingFilters by rememberSaveable { mutableStateOf(false) }
    val onSearch: (String) -> Unit = { onEvent(PlaceUiEvent.SearchChanged(it)) }
    val onCategory: (PlaceCategory?) -> Unit = { onEvent(PlaceUiEvent.CategorySelected(it)) }
    val onRetry = { onEvent(PlaceUiEvent.RetryClicked) }
    val onMore = { onEvent(PlaceUiEvent.MoreRequested) }
    val onSaveRequest: (PlaceMission) -> Unit = { onEvent(PlaceUiEvent.SaveClicked(it)) }
    val onSave = { onEvent(PlaceUiEvent.SaveConfirmed) }
    val onDismissSave = { onEvent(PlaceUiEvent.SaveDismissed) }
    val onDismissToast = { onEvent(PlaceUiEvent.NoticeDismissed) }
    val onSaved = { onEvent(PlaceUiEvent.SavedClicked) }
    val onHideSaved = { onEvent(PlaceUiEvent.SavedBackClicked) }
    val onBack = { onEvent(PlaceUiEvent.BackClicked) }
    LaunchedEffect(state.toast) { if (state.toast) { delay(3500); onDismissToast() } }
    Box(Modifier.fillMaxSize().background(KiriColor.Bg.Base).testTag("place_screen")) {
        Column {
            KiriAppBar(if (state.showingSaved) "버킷리스트" else "미션", onBack = if (state.showingSaved) onHideSaved else onBack)
            if (!state.showingSaved) {
                OutlinedTextField(value = state.query.keyword, onValueChange = onSearch,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp).heightIn(min = 56.dp).testTag("place_search"),
                    placeholder = { Text("장소나 활동을 검색해보세요", style = KiriDetailTypography.Body.copy(lineHeight = 24.sp)) },
                    textStyle = KiriDetailTypography.Body.copy(lineHeight = 24.sp), singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = KiriColor.Bg.Secondary,
                        focusedContainerColor = KiriColor.Bg.Secondary, unfocusedBorderColor = KiriColor.Bg.Secondary,
                        focusedBorderColor = KiriColor.Primary.Default, unfocusedPlaceholderColor = KiriColor.Text.Tertiary))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaceChip("전체", state.query.category == null, { onCategory(null) })
                    PlaceCategory.entries.forEach { category -> PlaceChip(category.label, state.query.category == category, { onCategory(category) }, Modifier.testTag("place_category_${category.name}")) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (state.showingSaved) "저장한 활동 ${state.savedItems.size}개" else if (state.loading) "활동 불러오는 중" else "활동 ${state.totalCount}개", style = KiriDetailTypography.Small,
                    color = if (!state.showingSaved && state.query.filters.activeCount > 0) KiriColor.Text.Accent else KiriColor.Text.Secondary,
                    modifier = Modifier.weight(1f).testTag("place_count"))
                if (!state.showingSaved) {
                    Box {
                        IconButton(onClick = { showingFilters = true }, modifier = Modifier.testTag("place_filters")) {
                            Surface(shape = RoundedCornerShape(100.dp), color = KiriColor.Bg.Tertiary, modifier = Modifier.size(28.dp)) {
                                Box(contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.kiri_filter_action), "상세 필터", Modifier.size(width = 8.dp, height = 8.421.dp),
                                    colorFilter = ColorFilter.tint(if (state.query.filters.activeCount > 0) KiriColor.Text.Accent else KiriColor.Text.Secondary)) }
                            }
                        }
                        if (state.query.filters.activeCount > 0) Surface(color = KiriColor.Primary.Default, shape = RoundedCornerShape(100.dp), modifier = Modifier.align(Alignment.TopEnd).size(16.dp).testTag("place_filter_count")) {
                            Box(contentAlignment = Alignment.Center) { Text("${state.query.filters.activeCount}", style = KiriDetailTypography.Small.copy(fontSize = 10.sp), color = KiriColor.Text.Inverse) }
                        }
                    }
                }
            }
            if (state.error != null && state.pending == null) {
                Text(state.error, Modifier.padding(horizontal = 18.dp), color = KiriColor.Status.Error, style = KiriDetailTypography.Small)
                TextButton(onClick = if (state.showingSaved) onSaved else if (state.items.isEmpty()) onRetry else onMore) { Text("다시 시도") }
            }
            val missions = if (state.showingSaved) state.savedItems else state.items
            if (state.loading && !state.showingSaved) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (missions.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (state.showingSaved) "저장한 활동이 없어요" else "검색 결과가 없어요", style = KiriDetailTypography.Title, color = KiriColor.Text.Strong)
                    Text(if (state.showingSaved) "마음에 드는 활동을 저장해보세요" else "다른 검색어나 카테고리로 둘러보세요", style = KiriDetailTypography.Small, color = KiriColor.Text.Secondary)
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("place_list"), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(missions, key = { it.id }) { mission -> PlaceMissionCard(mission, { onSaveRequest(mission) }, { onEvent(PlaceUiEvent.MissionClicked(mission.id)) }) }
                    if (!state.showingSaved && state.nextCursor != null) item {
                        LaunchedEffect(state.nextCursor) { onMore() }
                        if (state.loadingMore) CircularProgressIndicator(Modifier.padding(16.dp))
                        else TextButton(onClick = onMore) { Text("더 보기") }
                    }
                }
            }
        }
        if (state.toast) com.kiri.uikit.component.KiriToast(
            "버킷리스트에 저장되었습니다!",
            Modifier.align(Alignment.TopCenter).padding(top = 62.dp, start = 18.dp, end = 18.dp).fillMaxWidth().testTag("place_saved_toast"),
            actionLabel = "바로가기", onAction = onSaved,
        )
    }
    if (showingFilters) PlaceFilterSheet(state.query.filters, onDismiss = { showingFilters = false }, onApply = {
        onEvent(PlaceUiEvent.FiltersApplied(it))
        showingFilters = false
    })
    state.pending?.let { mission ->
        Dialog(onDismissRequest = onDismissSave, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.padding(horizontal = 22.dp).widthIn(max = 345.dp).fillMaxWidth().testTag("place_save_dialog"), color = KiriColor.Bg.Secondary, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 28.dp)) {
                    Text("버킷리스트에 저장하시겠습니까?", style = KiriDetailTypography.Title.copy(fontSize = 20.sp, lineHeight = 28.sp), color = KiriColor.Text.Strong)
                    Spacer(Modifier.height(20.dp))
                    Surface(color = KiriColor.Bg.Tertiary, shape = RoundedCornerShape(10.dp)) {
                        Column(Modifier.fillMaxWidth().heightIn(min = 90.dp).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(mission.title, style = KiriDetailTypography.Title, color = KiriColor.Text.Strong)
                            Spacer(Modifier.height(4.dp))
                            Text("이 활동은 버킷리스트에서 확인하실 수 있습니다", style = KiriDetailTypography.Small.copy(fontSize = 12.sp, lineHeight = 18.sp), color = KiriColor.Text.Tertiary)
                        }
                    }
                    state.error?.let { Text(it, style = KiriDetailTypography.Small, color = KiriColor.Status.Error) }
                    Spacer(Modifier.height(42.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PlaceDialogButton("취소", onDismissSave, false, !state.saving, Modifier.weight(1f))
                        PlaceDialogButton(if (state.saving) "저장 중…" else "저장하기", onSave, true, !state.saving, Modifier.weight(1f).testTag("place_confirm_save"))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.height(36.dp).semantics { this.selected = selected }, color = if (selected) KiriColor.Primary.Default.copy(alpha = .2f) else KiriColor.Bg.Tertiary, shape = RoundedCornerShape(100.dp)) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) { Text(label, style = KiriDetailTypography.Small.copy(fontWeight = FontWeight.Medium, lineHeight = 20.sp), color = if (selected) KiriColor.Text.Accent else KiriColor.Text.Secondary) }
    }
}
@Composable
private fun PlaceMissionCard(mission: PlaceMission, onSave: () -> Unit, onOpen: () -> Unit) {
    Surface(onClick = onOpen, color = KiriColor.Bg.Primary, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().testTag("place_card_${mission.id}")) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(91.dp).background(KiriColor.Bg.Elevated, RoundedCornerShape(10.dp))) {
                run {
                    val rating = mission.rating ?: "평가 전"
                    Surface(Modifier.padding(7.dp), color = KiriColor.Bg.Tertiary, shape = RoundedCornerShape(100.dp)) {
                        Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.place_star), null, Modifier.size(24.dp))
                            Text(rating, color = KiriColor.Text.Tertiary, style = KiriDetailTypography.Small.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = (-0.4).sp))
                        }
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(21.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(mission.title, color = KiriColor.Text.Strong, style = KiriDetailTypography.Title)
                    Text("${mission.place} · ${mission.region}", color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        List(2) { mission.tags.getOrNull(it) ?: "정보 없음" }.forEach { tag ->
                            Surface(color = KiriColor.Bg.Elevated, shape = RoundedCornerShape(5.dp), modifier = Modifier.weight(1f, fill = false)) {
                                Text(tag, Modifier.padding(horizontal = 8.dp, vertical = 6.dp), style = KiriDetailTypography.Small.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = (-0.48).sp), color = KiriColor.Text.Tertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { IconButton(onClick = onSave, enabled = !mission.saved, modifier = Modifier.requiredSize(48.dp).testTag("place_save_${mission.id}")) {
                        Image(painterResource(if (mission.saved) R.drawable.place_bookmark_filled else R.drawable.place_bookmark_outline), mission.saveLabel, Modifier.size(24.dp))
                    } }
                }
                TextButton(onClick = onSave, enabled = !mission.saved,
                    modifier = Modifier.align(Alignment.End)) {
                    Text(mission.saveLabel, style = KiriDetailTypography.Small)
                }
            }
        }
    }
}
@Composable
private fun PlaceDialogButton(label: String, onClick: () -> Unit, accent: Boolean, enabled: Boolean, modifier: Modifier) {
    Button(onClick, modifier.heightIn(min = 54.dp), enabled = enabled, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = if (accent) KiriColor.Primary.Default else KiriColor.Bg.Primary, contentColor = if (accent) KiriColor.Text.Inverse else KiriColor.Text.Primary), contentPadding = PaddingValues(8.dp)) {
        Text(label, style = KiriDetailTypography.Body.copy(fontWeight = FontWeight.Medium, lineHeight = 24.sp))
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun PlaceScreenPreview() { PlaceScreen(PlaceUiState(), {}) }
