package com.kiri.feature.detail.presentation

import com.kiri.feature.detail.DetailViewModel
import com.kiri.feature.detail.model.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.domain.detail.Condition
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.*

@Composable
fun ConditionCustomRoute(viewModel: DetailViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentBack by rememberUpdatedState(onBack)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    DetailUiEffect.NavigateBack -> currentBack()
                    DetailUiEffect.OpenCustom -> Unit
                    DetailUiEffect.OpenRecord -> Unit
                }
            }
        }
    }
    ConditionCustomScreen(state, viewModel::onEvent)
}

@Composable
fun ConditionCustomScreen(state: DetailUiState, onEvent: (DetailUiEvent) -> Unit) {
    val onBack = { onEvent(DetailUiEvent.CustomCancelled) }
    val onToggle: (Condition) -> Unit = { onEvent(DetailUiEvent.ConditionToggled(it)) }
    val onApply = { onEvent(DetailUiEvent.ApplyClicked) }
    BackHandler { if (!state.saving) onBack() }
    Column(Modifier.fillMaxSize().background(KiriColor.Bg.Base).testTag("condition_custom_screen")) {
        KiriDetailAppBar("컨디션 커스텀", onBack, enabled = !state.saving)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 28.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("컨디션 조정이 필요하신가요?", style = KiriDetailTypography.Body, color = KiriColor.Text.Secondary)
            Spacer(Modifier.height(5.dp))
            Text(state.detail?.title.orEmpty(), Modifier.padding(horizontal = 18.dp), style = KiriDetailTypography.Display, color = KiriColor.Text.Strong, textAlign = TextAlign.Center)
            Spacer(Modifier.height(48.dp))
            Text("예상 걸음수 ${state.detail?.stepsLabel ?: "정보 없음"}  ·  활동시간 ${state.detail?.durationLabel ?: "정보 없음"}", Modifier.padding(horizontal = 18.dp), style = KiriDetailTypography.Body, color = KiriColor.Text.Primary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(21.dp))
            Column(Modifier.widthIn(max = 480.dp).padding(horizontal = 36.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Condition.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { condition ->
                            KiriConditionCard(condition.label(), condition in state.draft, { onToggle(condition) }, Modifier.weight(1f).testTag("condition_${condition.name}"), enabled = !state.saving && state.detail != null)
                        }
                    }
                }
            }
            state.error?.let { Text(it, Modifier.padding(18.dp).testTag("custom_error"), color = KiriColor.Status.Error, style = KiriDetailTypography.Small) }
        }
        KiriDetailButton(if (state.saving) "적용 중…" else "커스텀 적용하기", onApply, Modifier.padding(horizontal = 18.dp, vertical = 15.dp).testTag("apply_custom"), custom = true, enabled = !state.saving && state.detail?.completed == false)
    }
}

private fun Condition.label(): String = when (this) {
    Condition.LESS_WALKING -> "걸음수를\n낮춰주세요"
    Condition.MORE_REST -> "휴식 시간을\n늘려주세요"
    Condition.LOWER_BUDGET -> "예산을\n낮춰주세요"
    Condition.AVOID_CROWDS -> "사람 많은 곳은\n피해주세요"
}
