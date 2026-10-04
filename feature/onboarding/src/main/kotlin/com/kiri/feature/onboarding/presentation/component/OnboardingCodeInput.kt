package com.kiri.feature.onboarding.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.feature.onboarding.R
import com.kiri.uikit.color.KiriColor

@Composable
fun OnboardingCodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
) {
    BasicTextField(
        value = code,
        // 영문은 대문자로 바꾸고 영문/숫자만 길이만큼 받는다
        onValueChange = { input ->
            onCodeChange(input.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(length))
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
        ),
        modifier = modifier.testTag("onboarding_code_input"),
        decorationBox = { innerTextField ->
            Box {
                // 실제 입력 필드는 보이지 않게 두고, 입력된 글자는 칸마다 직접 그린다
                Box(Modifier.matchParentSize().alpha(0f)) { innerTextField() }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(length) { index ->
                        CodeBox(char = code.getOrNull(index))
                    }
                }
            }
        },
    )
}

@Composable
private fun CodeBox(char: Char?) {
    if (char == null) {
        Image(
            painter = painterResource(R.drawable.ic_onboarding_code_box),
            contentDescription = null,
        )
        return
    }
    Box(
        modifier = Modifier
            .size(width = 43.dp, height = 56.dp)
            .background(KiriColor.IconDisabled, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // TODO: Figma에는 빈 칸 상태만 있어 입력된 글자 스타일은 임시값
        Text(
            text = char.toString(),
            style = TextStyle(
                color = KiriColor.TextStrong,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun OnboardingCodeInputPreview() {
    OnboardingCodeInput(code = "KI2", onCodeChange = {})
}
