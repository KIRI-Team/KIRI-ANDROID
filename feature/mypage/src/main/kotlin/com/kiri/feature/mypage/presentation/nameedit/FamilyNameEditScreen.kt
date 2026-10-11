package com.kiri.feature.mypage.presentation.nameedit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.mypage.FamilyNameEditViewModel
import com.kiri.feature.mypage.model.nameedit.NameEditUiEvent
import com.kiri.feature.mypage.model.nameedit.NameEditUiState
import com.kiri.uikit.color.KiriColor

@Composable
fun FamilyNameEditRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FamilyNameEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NameEditEffects(viewModel = viewModel, onNavigateBack = onNavigateBack)

    FamilyNameEditScreen(modifier = modifier, state = state, onEvent = viewModel::onEvent)
}

// 가족 기본 설정에서 가족 이름 옆 수정(펜) 아이콘을 누르면 여는 가족명 수정 화면
@Composable
fun FamilyNameEditScreen(
    state: NameEditUiState,
    onEvent: (NameEditUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    NameEditScreen(
        state = state,
        onEvent = onEvent,
        title = "가족명을 다시 입력해주세요.",
        subtitle = "가족명은 월 1회 수정 가능합니다",
        subtitleColor = KiriColor.Text.Tertiary,
        placeholder = "지금 가족명: ${state.currentName}",
        testTag = "family_name_edit_screen",
        modifier = modifier,
    )
}

@Preview(showSystemUi = true)
@Composable
private fun FamilyNameEditScreenPreview() {
    FamilyNameEditScreen(state = NameEditUiState(currentName = "끼리네가족"), onEvent = {})
}
