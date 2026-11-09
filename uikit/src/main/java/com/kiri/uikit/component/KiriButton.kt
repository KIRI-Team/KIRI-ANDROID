package com.kiri.uikit.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.uikit.color.KiriComponentColor
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.KiriTypography

enum class KiriButtonTone { Accent, Neutral }
enum class KiriButtonLayout { Full, Record, Inline }

internal data class KiriButtonColors(val container: Color, val content: Color)

// KIRI/Button: Default / Pressed / Disabled 상태별 색상
internal fun kiriButtonColors(
    tone: KiriButtonTone,
    enabled: Boolean,
    pressed: Boolean,
): KiriButtonColors = when (tone) {
    KiriButtonTone.Accent -> with(KiriComponentColor.Button.FilledAccent) {
        when {
            !enabled -> KiriButtonColors(BgDisabled, ContentDisabled)
            pressed -> KiriButtonColors(BgPressed, ContentPressed)
            else -> KiriButtonColors(BgDefault, ContentDefault)
        }
    }
    KiriButtonTone.Neutral -> with(KiriComponentColor.Button.FilledNeutral) {
        when {
            !enabled -> KiriButtonColors(BgDisabled, ContentDisabled)
            pressed -> KiriButtonColors(BgPressed, ContentPressed)
            else -> KiriButtonColors(BgDefault, ContentDefault)
        }
    }
}

@Composable
fun KiriButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String = "다음",
    tone: KiriButtonTone = KiriButtonTone.Accent,
    leadingIcon: (@Composable () -> Unit)? = null,
    testTag: String = "kiri_button",
    layout: KiriButtonLayout = KiriButtonLayout.Full,
    trailingIcon: (@Composable () -> Unit)? = null,
    // 기본 상태의 글자색을 바꾼다. 예) 일정 변경하기 버튼은 kiri-text-secondary (Figma 9898:21808)
    defaultContentColor: Color? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val baseColors = if (enabled && !pressed && tone == KiriButtonTone.Neutral) {
        when (layout) {
            KiriButtonLayout.Record -> KiriButtonColors(KiriColor.Bg.Secondary, KiriColor.Text.Strong)
            KiriButtonLayout.Inline -> KiriButtonColors(KiriColor.Bg.Primary, KiriColor.Text.Tertiary)
            KiriButtonLayout.Full -> kiriButtonColors(tone, enabled, pressed)
        }
    } else kiriButtonColors(tone, enabled, pressed)
    // 기본 상태에서만 글자색을 바꾸고, 눌림·비활성 색은 그대로 둔다
    val (containerColor, textColor) =
        if (defaultContentColor != null && enabled && !pressed) baseColors.copy(content = defaultContentColor) else baseColors
    // KIRI/Button 컴포넌트는 톤과 관계없이 같은 글자 스타일(Title/Medium)을 쓴다
    val textStyle = when (layout) {
        KiriButtonLayout.Full -> KiriTypography.TitleMedium
        KiriButtonLayout.Record -> KiriTypography.TitleLarge
        KiriButtonLayout.Inline -> KiriTypography.BodySmall
    }.copy(color = textColor)

    Box(
        modifier = modifier
            .then(if (layout == KiriButtonLayout.Inline) Modifier else Modifier.fillMaxWidth())
            .height(when (layout) { KiriButtonLayout.Full -> 54.dp; KiriButtonLayout.Record -> 48.dp; KiriButtonLayout.Inline -> 30.dp })
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            // 눌림 피드백은 Pressed 색상으로 표현해서 기본 리플은 끈다
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = when (layout) { KiriButtonLayout.Inline -> 12.dp; KiriButtonLayout.Record -> 10.dp; else -> 16.dp })
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides textColor) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (layout == KiriButtonLayout.Inline) 2.dp else 8.dp)) {
            leadingIcon?.invoke()
            Text(
                text = text,
                textAlign = TextAlign.Center,
                style = textStyle,
            )
            trailingIcon?.invoke()
          }
        }
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun KiriButtonPreview() {
    KiriButton(onClick = {}, enabled = false)
}
