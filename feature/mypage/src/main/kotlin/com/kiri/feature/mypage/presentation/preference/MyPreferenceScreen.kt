package com.kiri.feature.mypage.presentation.preference

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.mypage.MyPreferenceViewModel
import com.kiri.feature.mypage.model.preference.MyPreferenceUiEffect
import com.kiri.feature.mypage.model.preference.MyPreferenceUiEvent
import com.kiri.feature.mypage.model.preference.MyPreferenceUiState
import com.kiri.feature.mypage.model.preference.PreferenceActivity
import com.kiri.feature.mypage.model.preference.PreferenceCategory
import com.kiri.feature.mypage.model.preference.PreferenceDuration
import com.kiri.feature.mypage.model.preference.PreferencePrice
import com.kiri.feature.mypage.model.preference.PreferenceSpace
import com.kiri.uikit.KiriTypography
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.color.KiriComponentColor
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriTopBar

@Composable
fun MyPreferenceRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyPreferenceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                MyPreferenceUiEffect.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    MyPreferenceScreen(modifier = modifier, state = state, onEvent = viewModel::onEvent)
}

// 내 취향 설정 (Figma 9376:17846). 항목별 칩을 고르고 아래 버튼으로 저장한다
@Composable
fun MyPreferenceScreen(
    state: MyPreferenceUiState,
    onEvent: (MyPreferenceUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .testTag("my_preference_screen"),
    ) {
        KiriTopBar(onBackClick = { onEvent(MyPreferenceUiEvent.BackClicked) }, title = "내 취향 설정")
        // 작은 화면에서도 모든 항목을 고를 수 있도록 항목 영역만 스크롤한다
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 30.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${state.nickname}님, 가족과 어떤 시간을\n함께하고 싶나요?",
                    style = displayLarge,
                    color = KiriColor.Text.Strong,
                )
                Text(
                    text = "내 취향을 바탕으로 우리 가족에게 어울리는 활동을 추천해드려요.",
                    style = labelMedium,
                    color = KiriColor.Text.Secondary,
                )
            }
            PreferenceSection(title = "선호 활동 · 최대 ${MyPreferenceUiState.MAX_CATEGORIES}개", rowSpacing = 8) {
                CATEGORY_ROWS.forEach { row ->
                    ChipRow {
                        row.forEach { category ->
                            PreferenceChip(
                                label = category.label,
                                selected = category in state.categories,
                                onClick = { onEvent(MyPreferenceUiEvent.CategoryClicked(category)) },
                                multiSelect = true,
                                testTag = "preference_category_${category.name}",
                            )
                        }
                    }
                }
            }
            SingleChoiceSection(
                title = "공간",
                options = PreferenceSpace.entries,
                selected = state.space,
                label = { it.label },
                onClick = { onEvent(MyPreferenceUiEvent.SpaceClicked(it)) },
                testTagPrefix = "preference_space",
            )
            SingleChoiceSection(
                title = "활동량",
                options = PreferenceActivity.entries,
                selected = state.activity,
                label = { it.label },
                onClick = { onEvent(MyPreferenceUiEvent.ActivityClicked(it)) },
                testTagPrefix = "preference_activity",
            )
            SingleChoiceSection(
                title = "비용",
                options = PreferencePrice.entries,
                selected = state.price,
                label = { it.label },
                onClick = { onEvent(MyPreferenceUiEvent.PriceClicked(it)) },
                testTagPrefix = "preference_price",
            )
            // Figma 는 활동시간만 줄 사이를 12dp 로 둔다
            PreferenceSection(title = "활동시간", rowSpacing = 12) {
                DURATION_ROWS.forEach { row ->
                    ChipRow {
                        row.forEach { duration ->
                            PreferenceChip(
                                label = duration.label,
                                selected = duration == state.duration,
                                onClick = { onEvent(MyPreferenceUiEvent.DurationClicked(duration)) },
                                testTag = "preference_duration_${duration.name}",
                            )
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KiriButton(
                onClick = { onEvent(MyPreferenceUiEvent.SaveClicked) },
                modifier = Modifier.padding(horizontal = 18.dp),
                text = "취향 저장하기",
            )
            Text(
                text = "다음 추천부터 반영돼요. 이미 제공된 주간 미션은 바뀌지 않아요.",
                style = labelSmall,
                color = KiriColor.Text.Secondary,
            )
        }
    }
}

// 한 줄에 한 항목만 고르는 섹션 (공간·활동량·비용)
@Composable
private fun <T : Enum<T>> SingleChoiceSection(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onClick: (T) -> Unit,
    testTagPrefix: String,
) {
    PreferenceSection(title = title, rowSpacing = 0) {
        ChipRow {
            options.forEach { option ->
                PreferenceChip(
                    label = label(option),
                    selected = option == selected,
                    onClick = { onClick(option) },
                    testTag = "${testTagPrefix}_${option.name}",
                )
            }
        }
    }
}

@Composable
private fun PreferenceSection(
    title: String,
    rowSpacing: Int,
    chips: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = KiriTypography.TitleMedium, color = KiriColor.Text.Primary)
        Column(verticalArrangement = Arrangement.spacedBy(rowSpacing.dp)) { chips() }
    }
}

@Composable
private fun ChipRow(chips: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
}

// KIRI/Chip (Default·Selected). 선호 활동은 여러 개를 고르는 체크박스, 나머지는 하나만 고르는 라디오 버튼으로 읽힌다
@Composable
private fun PreferenceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    multiSelect: Boolean = false,
) {
    val selection = if (multiSelect) {
        Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    }
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(if (selected) KiriComponentColor.Chip.BgSelected else KiriComponentColor.Chip.BgDefault)
            .then(selection)
            .padding(horizontal = 16.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = labelMedium,
            color = if (selected) KiriComponentColor.Chip.ContentSelected else KiriComponentColor.Chip.ContentDefault,
            maxLines = 1,
        )
    }
}

// Figma 의 줄 구성을 그대로 따른다
private val CATEGORY_ROWS = PreferenceCategory.entries.chunked(3)
private val DURATION_ROWS = PreferenceDuration.entries.chunked(3)

// KIRI/Display/Large, KIRI/Label/Medium, KIRI/Label/Small
private val displayLarge = KiriTypography.TitleMedium.copy(fontSize = 28.sp, lineHeight = 40.sp, letterSpacing = (-0.02).em)
private val labelMedium = KiriTypography.TitleMedium.copy(fontSize = 14.sp, lineHeight = 20.sp)
private val labelSmall = KiriTypography.TitleMedium.copy(fontSize = 12.sp, lineHeight = 17.sp)

@Preview(showSystemUi = true)
@Composable
private fun MyPreferenceScreenPreview() {
    MyPreferenceScreen(
        state = MyPreferenceUiState(
            nickname = "쥬123",
            categories = setOf(PreferenceCategory.EXHIBITION, PreferenceCategory.FOOD_CAFE),
            space = PreferenceSpace.INDOOR,
            activity = PreferenceActivity.LIGHT,
            price = PreferencePrice.FREE,
            duration = PreferenceDuration.WITHIN_1H,
        ),
        onEvent = {},
    )
}
