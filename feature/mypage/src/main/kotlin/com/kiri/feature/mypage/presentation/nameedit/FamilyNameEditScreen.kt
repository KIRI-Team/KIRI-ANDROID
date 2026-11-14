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
    NameEditEffects(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        saveErrorText = "가족명을 저장하지 못했어요. 잠시 후 다시 시도해주세요.",
    )

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
        // Handoff 10298:38300
        title = "가족명을 수정해요",
        subtitle = "가족명은 월 1회 수정 가능합니다",
        subtitleColor = KiriColor.Text.Tertiary,
        placeholder = "현재 가족 이름: ${state.currentName}",
        testTag = "family_name_edit_screen",
        appBarTitle = "가족명 수정",
        duplicatedText = "이미 존재하는 가족 이름입니다.",
        modifier = modifier,
    )
}

@Preview(showSystemUi = true)
@Composable
private fun FamilyNameEditScreenPreview() {
    FamilyNameEditScreen(state = NameEditUiState(currentName = "끼리네가족"), onEvent = {})
}
