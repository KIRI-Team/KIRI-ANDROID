package com.kiri.uikit

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.kiri.uikit.color.KiriColor

private val KiriDarkColors = darkColorScheme(
    primary = KiriColor.Primary.Default,
    onPrimary = KiriColor.Text.OnGreen,
    background = KiriColor.Bg.Base,
    onBackground = KiriColor.Text.Strong,
    surface = KiriColor.Bg.Primary,
    onSurface = KiriColor.Text.Strong,
    surfaceVariant = KiriColor.Bg.Secondary,
    onSurfaceVariant = KiriColor.Text.Secondary,
    outline = KiriColor.Border.Default,
)

@Composable
fun KiriTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KiriDarkColors, content = content)
}
