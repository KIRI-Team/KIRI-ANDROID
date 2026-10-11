package com.kiri.uikit.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.kiri.uikit.R

// 1초에 한 바퀴 도는 로딩 표시
@Composable
fun KiriLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "loading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "loading_rotation",
    )

    Image(
        painter = painterResource(R.drawable.ic_kiri_loading),
        contentDescription = "로딩 중",
        modifier = modifier
            .rotate(rotation)
            .testTag("kiri_loading_indicator"),
    )
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun KiriLoadingIndicatorPreview() {
    KiriLoadingIndicator()
}
