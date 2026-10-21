package com.kiri.feature.mypage.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.kiri.uikit.color.KiriComponentColor

// KIRI/Input 한 줄 입력창. errors가 있으면 Error 상태로 그리고 입력창 아래에 오류 문구를 보여준다
// readOnly면 직접 입력할 수 없고 값만 보여준다 (예: 주소 검색으로 채우는 칸)
@Composable
fun MypageInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errors: List<String> = emptyList(),
    enabled: Boolean = true,
    readOnly: Boolean = false,
    testTag: String = "mypage_input_box",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val inputTextStyle = TextStyle(
        fontFamily = com.kiri.uikit.KiriFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
        letterSpacing = (-0.04).em,
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isError = errors.isNotEmpty()
    val shape = RoundedCornerShape(16.dp)
    val input = KiriComponentColor.Input
    // Disabled > Error > Focus > Filled > Default 순으로 우선한다
    val background = when {
        !enabled -> input.BgDisabled
        isError -> input.BgError
        isFocused -> input.BgFocus
        value.isNotEmpty() -> input.BgFilled
        else -> input.BgDefault
    }
    val borderColor = when {
        !enabled -> input.BorderDisabled
        isError -> input.BorderError
        isFocused -> input.BorderFocus
        value.isNotEmpty() -> input.BorderFilled
        else -> input.BorderDefault
    }
    // 최신 Figma는 Error에만 1dp 외곽선, Focus는 커서로 표시한다.
    val borderWidth = if (enabled && isError) 1.dp else 0.dp
    val contentColor = when {
        !enabled -> input.ContentDisabled
        isError -> input.ContentError
        isFocused -> input.ContentFocus
        else -> input.ContentFilled
    }
    val placeholderColor = when {
        !enabled -> input.PlaceholderDisabled
        isError -> input.PlaceholderError
        isFocused -> input.PlaceholderFocus
        else -> input.PlaceholderDefault
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            textStyle = inputTextStyle.copy(color = contentColor),
            cursorBrush = SolidColor(input.BorderFocus),
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(color = background, shape = shape)
                        .border(width = borderWidth, color = borderColor, shape = shape)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(text = placeholder, style = inputTextStyle.copy(color = placeholderColor))
                        }
                        innerTextField()
                    }
                    trailingIcon?.invoke()
                }
            },
        )
        // 오류가 없으면 아래 여백도 차지하지 않도록 그리지 않는다
        if (isError) Column(
            modifier = Modifier.padding(start = 24.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            errors.forEach { error ->
                Text(
                    text = error,
                    style = TextStyle(
                        color = input.HelperError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 1.45.em,
                        letterSpacing = (-0.01).em,
                    ),
                )
            }
        }
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun MypageInputBoxPreview() {
    MypageInputBox(
        value = "주",
        onValueChange = {},
        errors = listOf("최소 2자 ~ 최대 10자 이내로 입력해주세요"),
    )
}
