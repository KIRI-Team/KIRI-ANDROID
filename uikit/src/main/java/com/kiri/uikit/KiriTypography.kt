package com.kiri.uikit

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val KiriFontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
)

/** Figma KIRI typography: 자간 -4%, Android 기본 폰트 여백 제외. */
object KiriTypography {
    private val base = TextStyle(
        fontFamily = KiriFontFamily,
        letterSpacing = (-0.04).em,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )
    val TitleLarge = base.copy(fontSize = 18.sp, lineHeight = 27.sp, fontWeight = FontWeight.Medium)
    val TitleMedium = base.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    val AppBar = TitleLarge.copy(fontWeight = FontWeight.SemiBold)
    val BodyLarge = base.copy(fontSize = 16.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal)
    val BodyMedium = base.copy(fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal)
    val BodySmall = base.copy(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    val Value = base.copy(fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium)

    internal val Material = Typography().let { defaults ->
        fun TextStyle.kiri() = copy(fontFamily = KiriFontFamily, letterSpacing = (-0.04).em,
            platformStyle = base.platformStyle, lineHeightStyle = base.lineHeightStyle)
        defaults.copy(
            displayLarge = defaults.displayLarge.kiri(), displayMedium = defaults.displayMedium.kiri(), displaySmall = defaults.displaySmall.kiri(),
            headlineLarge = defaults.headlineLarge.kiri(), headlineMedium = defaults.headlineMedium.kiri(), headlineSmall = defaults.headlineSmall.kiri(),
            titleLarge = TitleLarge, titleMedium = TitleMedium, titleSmall = TitleMedium,
            bodyLarge = BodyLarge, bodyMedium = BodyMedium, bodySmall = BodySmall,
            labelLarge = TitleMedium, labelMedium = BodySmall, labelSmall = BodySmall,
        )
    }
}
