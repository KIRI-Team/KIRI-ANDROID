package com.kiri.feature.record.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.feature.record.R
import com.kiri.feature.record.RecordViewModel
import com.kiri.feature.record.model.*
import com.kiri.feature.record.presentation.bucket.RecordBucketList
import com.kiri.feature.record.presentation.calendar.RecordCalendar
import com.kiri.uikit.KiriTheme
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography

@Composable
fun RecordRoute(
    onScheduleClick: (String) -> Unit = {},
    onBucketClick: (String) -> Unit = {},
    onEntryClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    onServerScheduleClick: (Long) -> Unit = {},
    viewModel: RecordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentScheduleClick by rememberUpdatedState(onScheduleClick)
    val currentBucketClick by rememberUpdatedState(onBucketClick)
    val currentEntryClick by rememberUpdatedState(onEntryClick)
    val currentServerScheduleClick by rememberUpdatedState(onServerScheduleClick)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onEvent(RecordUiEvent.RetryClicked)
            viewModel.effect.collect { effect ->
                when (effect) {
                    is RecordUiEffect.OpenSchedule -> currentScheduleClick(effect.id)
                    is RecordUiEffect.OpenBucket -> currentBucketClick(effect.id)
                    is RecordUiEffect.OpenEntry -> currentEntryClick(effect.id)
                    is RecordUiEffect.OpenServerSchedule -> currentServerScheduleClick(effect.scheduleId)
                }
            }
        }
    }
    RecordScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

@Composable
fun RecordScreen(state: RecordUiState, onEvent: (RecordUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().background(KiriColor.Bg.Base).testTag("record_screen"),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 46.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RecordTab.entries.forEach { tab ->
                Column(
                    Modifier.weight(1f).selectable(selected = state.tab == tab, role = Role.Tab,
                        onClick = { onEvent(RecordUiEvent.TabSelected(tab)) }).testTag("record_tab_${tab.name}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(tab.label, color = if (state.tab == tab) KiriColor.Text.Strong else KiriColor.Text.Tertiary,
                        style = KiriDetailTypography.Title, modifier = Modifier.padding(bottom = 8.dp))
                    Image(painterResource(if (state.tab == tab) R.drawable.record_tab_active else R.drawable.record_tab_inactive),
                        null, Modifier.fillMaxWidth().height(1.dp), contentScale = ContentScale.FillBounds)
                }
            }
        }
        if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(24.dp))
        state.error?.let {
            Text(it, color = KiriColor.Status.Error, modifier = Modifier.padding(18.dp))
            TextButton(onClick = { onEvent(RecordUiEvent.RetryClicked) }) { Text("다시 시도") }
        }
        when (state.tab) {
            RecordTab.CALENDAR -> RecordCalendar(state, onEvent, Modifier.weight(1f))
            RecordTab.BUCKET -> RecordBucketList(state, onEvent, Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarPreview() { KiriTheme { RecordScreen(RecordPreviewData.state(), {}) } }

@Preview(showBackground = true)
@Composable
private fun BucketPreview() { KiriTheme { RecordScreen(RecordPreviewData.state().copy(tab = RecordTab.BUCKET), {}) } }
