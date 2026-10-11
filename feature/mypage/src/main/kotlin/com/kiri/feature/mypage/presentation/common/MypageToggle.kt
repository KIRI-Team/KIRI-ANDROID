package com.kiri.feature.mypage.presentation.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.uikit.color.KiriColor

// 42×24 토글. 켜지면 동그라미가 오른쪽(밝은 회색), 꺼지면 왼쪽(어두운 회색)으로 간다
@Composable
fun MypageToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "mypage_toggle",
) {
    val knobOffset by animateDpAsState(targetValue = if (checked) 18.dp else 0.dp, label = "knobOffset")

    Box(
        modifier = modifier
            .size(width = 42.dp, height = 24.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .background(KiriColor.Bg.Primary, RoundedCornerShape(12.dp))
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(3.dp)
            .testTag(testTag),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(18.dp)
                .background(if (checked) KiriColor.Icon.Primary else KiriColor.Icon.Disabled, CircleShape),
        )
    }
}

// 비활성화된 설정을 흐리게 보여줄 때 쓰는 투명도
internal const val DISABLED_ALPHA = 0.4f

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun MypageToggleOnPreview() {
    MypageToggle(checked = true, onCheckedChange = {})
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun MypageToggleOffPreview() {
    MypageToggle(checked = false, onCheckedChange = {})
}
