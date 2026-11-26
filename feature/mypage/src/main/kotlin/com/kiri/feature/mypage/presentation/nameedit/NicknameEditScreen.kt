package com.kiri.feature.mypage.presentation.nameedit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.mypage.NicknameEditViewModel
import com.kiri.feature.mypage.model.nameedit.NameEditUiEvent
import com.kiri.feature.mypage.model.nameedit.NameEditUiState

@Composable
fun NicknameEditRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NicknameEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NameEditEffects(viewModel = viewModel, onNavigateBack = onNavigateBack)

    NicknameEditScreen(modifier = modifier, state = state, onEvent = viewModel::onEvent)
}

// 마이페이지 프로필, 가족 구성원 카드의 수정(펜) 아이콘을 누르면 여는 닉네임 수정 화면
@Composable
fun NicknameEditScreen(
    state: NameEditUiState,
    onEvent: (NameEditUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    NameEditScreen(
        state = state,
        onEvent = onEvent,
        title = "닉네임을 입력하세요",
        subtitle = "닉네임은 월 1회 수정 가능합니다",
        placeholder = "지금 닉네임: ${state.currentName}",
        testTag = "nickname_edit_screen",
        // Handoff 10298:38187
        appBarTitle = "기본 정보",
        duplicatedText = "이미 사용 중인 닉네임입니다.",
        modifier = modifier,
    )
}

@Preview(showSystemUi = true)
@Composable
private fun NicknameEditScreenPreview() {
    NicknameEditScreen(state = NameEditUiState(currentName = "쥬123"), onEvent = {})
}
