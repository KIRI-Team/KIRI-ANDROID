package com.kiri.feature.mypage.presentation.nameedit

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiri.domain.model.nickname.NicknameRule
import com.kiri.domain.model.nickname.NicknameValidator
import com.kiri.feature.mypage.NameEditViewModel
import com.kiri.feature.mypage.model.nameedit.NameEditUiEffect
import com.kiri.feature.mypage.model.nameedit.NameEditUiEvent
import com.kiri.feature.mypage.model.nameedit.NameEditUiState
import com.kiri.feature.mypage.presentation.common.MypageInputBox
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriTitle
import com.kiri.uikit.component.KiriTopBar

// 닉네임·가족명 수정 Route가 함께 쓰는 effect 처리
@Composable
internal fun NameEditEffects(
    viewModel: NameEditViewModel,
    onNavigateBack: () -> Unit,
    saveErrorText: String = "저장하지 못했어요. 잠시 후 다시 시도해주세요.",
) {
    val context = LocalContext.current
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                NameEditUiEffect.NavigateBack -> currentOnNavigateBack()
                NameEditUiEffect.ShowSaveError -> Toast.makeText(context, saveErrorText, Toast.LENGTH_SHORT).show()
                is NameEditUiEffect.ShowMessage -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// 닉네임·가족명 수정 화면이 함께 쓰는 틀. 제목과 안내 문구만 화면마다 다르다
// duplicatedText 는 서버가 이미 있는 이름이라고 답했을 때 입력창 아래에 보여준다
@Composable
internal fun NameEditScreen(
    state: NameEditUiState,
    onEvent: (NameEditUiEvent) -> Unit,
    title: String,
    subtitle: String,
    placeholder: String,
    testTag: String,
    modifier: Modifier = Modifier,
    subtitleColor: Color = KiriColor.Text.Primary,
    duplicatedText: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .imePadding()
            .testTag(testTag),
    ) {
        KiriTopBar(onBackClick = { onEvent(NameEditUiEvent.BackClicked) })
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            KiriTitle(
                title = title,
                subtitle = subtitle,
                subtitleColor = subtitleColor,
                titleFontWeight = FontWeight.Normal,
            )
            Spacer(Modifier.height(38.dp))
            MypageInputBox(
                value = state.name,
                onValueChange = { onEvent(NameEditUiEvent.NameChanged(it)) },
                placeholder = placeholder,
                errors = nameErrors(state.violations, state.maxLength) +
                    listOfNotNull(duplicatedText?.takeIf { state.isDuplicated }),
            )
        }
        KiriButton(
            onClick = { onEvent(NameEditUiEvent.NextClicked) },
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
        )
    }
}

// 지키지 못한 규칙만 오류 문구로 보여준다. 길이 문구는 화면마다 다른 최대 길이를 쓴다
private fun nameRuleTexts(maxLength: Int) = listOf(
    NicknameRule.ALLOWED_LETTERS to "한글/영어/숫자만 입력할 수 있어요",
    NicknameRule.LENGTH to "최소 ${NicknameValidator.MIN_LENGTH}자 ~ 최대 ${maxLength}자 이내로 입력해주세요",
    NicknameRule.NO_SPECIAL_CHARACTERS to "특수 문자는 입력할 수 없어요",
)

internal fun nameErrors(violations: Set<NicknameRule>, maxLength: Int = NicknameValidator.MAX_LENGTH): List<String> =
    nameRuleTexts(maxLength).filter { (rule, _) -> rule in violations }.map { (_, text) -> text }
