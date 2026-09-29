package com.kiri.feature.onboarding.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    guides: List<String>,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = OnboardingColors.Grayscale100,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 1.45.em,
                letterSpacing = (-0.01).em,
            ),
            cursorBrush = SolidColor(OnboardingColors.Grayscale100),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_input_box"),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OnboardingColors.Grayscale700, RoundedCornerShape(8.dp))
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                color = OnboardingColors.Grayscale600,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 1.45.em,
                                letterSpacing = (-0.01).em,
                            ),
                        )
                    }
                    innerTextField()
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
                        color = OnboardingColors.Grayscale600,
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

@Preview(backgroundColor = 0xFF242426, showBackground = true)
@Composable
private fun OnboardingInputBoxPreview() {
    OnboardingInputBox(
        value = "주연주",
        onValueChange = {},
        guides = listOf("한글/영어/숫자 혼합 가능", "최소 2자 ~ 최대 10자 이내", "특수 문자 입력 불가능"),
    )
}
