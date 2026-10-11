package com.kiri.feature.mypage.presentation.suggestion

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.mypage.SuggestionViewModel
import com.kiri.feature.mypage.model.common.MypageMenu
import com.kiri.feature.mypage.model.suggestion.SuggestionUiEffect
import com.kiri.feature.mypage.model.suggestion.SuggestionUiEvent
import com.kiri.feature.mypage.model.suggestion.SuggestionUiState
import com.kiri.feature.mypage.presentation.suggestion.component.SuggestionPhotoAddTile
import com.kiri.feature.mypage.presentation.suggestion.component.SuggestionPhotoTile
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriButtonTone
import com.kiri.uikit.component.KiriTopBar

@Composable
fun SuggestionRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SuggestionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    val onPicked: (List<Uri>) -> Unit = { uris ->
        viewModel.onEvent(SuggestionUiEvent.PhotosPicked(uris.map(Uri::toString)))
    }
    // 여러 장 선택기는 2장 이상만 받으므로, 1장 남았을 때는 한 장 선택기를 쓴다
    // 남은 장수보다 많이 골라도 ViewModel 이 남은 장수만큼만 붙인다
    val pickMultiple = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(SuggestionUiState.MAX_PHOTO_COUNT),
        onResult = onPicked,
    )
    val pickSingle = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        onPicked(listOfNotNull(uri))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SuggestionUiEffect.OpenPhotoPicker -> {
                    val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    if (effect.maxCount >= 2) pickMultiple.launch(request) else pickSingle.launch(request)
                }
                SuggestionUiEffect.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    SuggestionScreen(modifier = modifier, state = state, onEvent = viewModel::onEvent)
}

// 건의함. 내용(최대 500자)과 관련 사진을 적어 보낸다
@Composable
fun SuggestionScreen(
    state: SuggestionUiState,
    onEvent: (SuggestionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .imePadding()
            .testTag("suggestion_screen"),
    ) {
        KiriTopBar(
            onBackClick = { onEvent(SuggestionUiEvent.BackClicked) },
            title = MypageMenu.SUGGESTION.title,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(39.dp))
            Text(
                text = "끼리가 더 발전할 수 있도록\n얘기해주세요!",
                style = TextStyle(
                    color = KiriColor.Text.Strong,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 40.sp,
                    letterSpacing = (-0.02).em,
                ),
            )
            Spacer(Modifier.height(36.dp))
            SectionLabel(text = "내용")
            Spacer(Modifier.height(8.dp))
            ContentBox(
                content = state.content,
                countText = state.contentCountText,
                onContentChange = { onEvent(SuggestionUiEvent.ContentChanged(it)) },
            )
            Spacer(Modifier.height(36.dp))
            SectionLabel(text = "사진")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.canAddPhoto) {
                    SuggestionPhotoAddTile(onClick = { onEvent(SuggestionUiEvent.PhotoAddClicked) })
                }
                state.photoUris.forEach { uri ->
                    SuggestionPhotoTile(
                        uri = uri,
                        onRemoveClick = { onEvent(SuggestionUiEvent.PhotoRemoveClicked(uri)) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "관련 화면을 함께 보내주시면 도움이 돼요.",
                style = TextStyle(
                    color = KiriColor.Text.Tertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.sp,
                    letterSpacing = (-0.04).em,
                ),
            )
            Spacer(Modifier.height(24.dp))
        }
        // 내용이 비어 있으면 디자인처럼 Neutral, 내용을 적으면 Accent 로 바뀐다
        KiriButton(
            onClick = { onEvent(SuggestionUiEvent.SendClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
            text = "의견 보내기",
            tone = if (state.canSend) KiriButtonTone.Accent else KiriButtonTone.Neutral,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = TextStyle(
            color = KiriColor.Text.Strong,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 27.sp,
            letterSpacing = (-0.04).em,
        ),
    )
}

// 여러 줄 입력칸. 오른쪽 아래에 글자 수(0/500)를 보여준다
@Composable
private fun ContentBox(
    content: String,
    countText: String,
    onContentChange: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    val textStyle = TextStyle(
        color = KiriColor.Text.Strong,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
        letterSpacing = (-0.04).em,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(149.dp)
            .background(KiriColor.Bg.Secondary, shape)
            .then(if (isFocused) Modifier.border(1.dp, KiriColor.Border.Strong, shape) else Modifier)
            .padding(start = 23.dp, end = 23.dp, top = 25.dp, bottom = 24.dp),
    ) {
        BasicTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("suggestion_content_input"),
            textStyle = textStyle,
            cursorBrush = SolidColor(KiriColor.Primary.Default),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box {
                    if (content.isEmpty()) {
                        Text(
                            text = "끼리를 사용하면서 말해주고 싶으셨던 점에 대해\n자유롭게 얘기해주세요!",
                            style = textStyle.copy(color = KiriColor.Text.Tertiary),
                        )
                    }
                    innerTextField()
                }
            },
        )
        Text(
            text = countText,
            modifier = Modifier
                .align(Alignment.End)
                .testTag("suggestion_content_count"),
            style = TextStyle(
                color = KiriColor.Text.Tertiary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.04).em,
            ),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SuggestionScreenPreview() {
    SuggestionScreen(state = SuggestionUiState(), onEvent = {})
}
