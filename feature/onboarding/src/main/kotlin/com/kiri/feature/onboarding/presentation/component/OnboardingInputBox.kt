package com.kiri.feature.onboarding.presentation.component

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

@Composable
fun OnboardingInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    guides: List<String>,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val inputTextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
        letterSpacing = (-0.04).em,
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val state = onboardingInputState(
        enabled = enabled,
        isError = isError,
        isFocused = isFocused,
        isEmpty = value.isEmpty(),
    )
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = inputTextStyle.copy(color = state.contentColor()),
            cursorBrush = SolidColor(KiriComponentColor.Input.BorderFocus),
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_input_box"),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(color = state.background(), shape = shape)
                        .border(width = state.borderWidth(), color = state.borderColor(), shape = shape)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = inputTextStyle.copy(color = state.placeholderColor()),
                            )
                        }
                        innerTextField()
                    }
                    trailingIcon?.invoke()
                }
            },
        )
        Column(
            modifier = Modifier.padding(start = 24.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            guides.forEach { guide ->
                Text(
                    text = "✓ $guide",
                    style = TextStyle(
                        color = if (enabled) KiriComponentColor.Input.HelperDefault else KiriComponentColor.Input.HelperDisabled,
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
private fun OnboardingInputBoxPreview() {
    OnboardingInputBox(
        value = "주연주",
        onValueChange = {},
        guides = listOf("한글/영어/숫자 혼합 가능", "최소 2자 ~ 최대 10자 이내", "특수 문자 입력 불가능"),
    )
}
