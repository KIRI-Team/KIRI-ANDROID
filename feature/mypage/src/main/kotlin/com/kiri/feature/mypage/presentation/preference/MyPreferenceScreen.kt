package com.kiri.feature.mypage.presentation.preference

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.domain.place.PlaceCategory
import com.kiri.domain.preferences.*
import com.kiri.feature.mypage.*
import com.kiri.uikit.KiriTypography
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.*

@Composable
fun MyPreferenceRoute(onBackClick: () -> Unit, viewModel: PreferenceViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MyPreferenceScreen(onBackClick, state = state, onToggle = viewModel::toggle, onChange = viewModel::change, onSave = viewModel::save, onRetry = { viewModel.reload() })
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyPreferenceScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier, state: PreferenceUiState = PreferenceUiState(loading = false), onToggle: (PlaceCategory) -> Unit = {}, onChange: (UserPreferences) -> Unit = {}, onSave: () -> Unit = {}, onRetry: () -> Unit = {}) {
    Column(modifier.fillMaxSize().background(KiriColor.Bg.Base).testTag("my_preference_screen")) {
        KiriDetailAppBar("내 취향 설정", onBackClick)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (state.loading) CircularProgressIndicator()
            state.error?.let { Text(it, color = KiriColor.Status.Error); TextButton(onRetry) { Text("다시 불러오기") } }
            if (state.saved) Text("취향이 저장되었어요.", color = KiriColor.Text.Accent)
            Text("선호 활동 · 최대 3개", style = KiriTypography.TitleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PlaceCategory.entries.forEach { category -> FilterChip(category in state.value.categories, { onToggle(category) }, { Text(category.label) }, enabled = !state.loading && !state.saving, modifier = Modifier.testTag("preference_${category.name}")) } }
            PreferenceOptions("실내·야외", Environment.entries, state.value.environment, { it.label }, { onChange(state.value.copy(environment = it)) }, !state.loading && !state.saving)
            PreferenceOptions("활동량", Intensity.entries, state.value.intensity, { it.label }, { onChange(state.value.copy(intensity = it)) }, !state.loading && !state.saving)
            PreferenceOptions("비용", Budget.entries, state.value.budget, { it.label }, { onChange(state.value.copy(budget = it)) }, !state.loading && !state.saving)
            PreferenceOptions("활동시간", ActivityDuration.entries, state.value.duration, { it.label }, { onChange(state.value.copy(duration = it)) }, !state.loading && !state.saving)
        }
        KiriButton(onSave, Modifier.padding(18.dp), enabled = state.value.valid && !state.loading && !state.saving, text = if (state.saving) "저장 중…" else "저장하기", testTag = "preference_save")
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> PreferenceOptions(title: String, options: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit, enabled: Boolean) {
    Column { Text(title, style = KiriTypography.TitleLarge); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { options.forEach { option -> FilterChip(option == selected, { onSelect(option) }, { Text(label(option)) }, enabled = enabled) } } }
}
