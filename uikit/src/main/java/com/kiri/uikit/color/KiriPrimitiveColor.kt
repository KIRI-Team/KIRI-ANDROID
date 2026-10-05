package com.kiri.uikit.color

import androidx.compose.ui.graphics.Color

// Figma: 02 / Primitive Colors (KIRI/Color/Primitive)
// 화면에서는 직접 쓰지 않고 KiriColor(시맨틱), KiriComponentColor(컴포넌트)를 통해 사용한다
object KiriPrimitiveColor {
    val Neutral0 = Color(0xFFFFFFFF)
    val Neutral50 = Color(0xFFF7F7F7)
    val Neutral100 = Color(0xFFEEEEEE)
    val Neutral200 = Color(0xFFD9D9D9)
    val Neutral300 = Color(0xFFB3B3B3)
    val Neutral400 = Color(0xFF8C8C8C)
    val Neutral500 = Color(0xFF737373)
    val Neutral600 = Color(0xFF595959)
    val Neutral700 = Color(0xFF404040)
    val Neutral800 = Color(0xFF333333)
    val Neutral850 = Color(0xFF2B2B2B)
    val Neutral900 = Color(0xFF242424)
    val Neutral950 = Color(0xFF1A1A1A)
    val Neutral1000 = Color(0xFF000000)

    val Green50 = Color(0xFFF3FDE9)
    val Green100 = Color(0xFFE4FACB)
    val Green200 = Color(0xFFCDF6A5)
    val Green300 = Color(0xFFB6F17C)
    val Green400 = Color(0xFFA6ED62)
    val Green500 = Color(0xFF9BEA51)
    val Green600 = Color(0xFF85D641)
    val Green700 = Color(0xFF6DB530)
    val Green800 = Color(0xFF568F27)
    val Green900 = Color(0xFF3C641C)

    val Blue500 = Color(0xFF6BAAF2)
    val Blue900 = Color(0xFF20364F)

    val Red500 = Color(0xFFF36B6B)
    val Red900 = Color(0xFF4A2424)

    val Yellow500 = Color(0xFFF1C75B)
    val Yellow900 = Color(0xFF463A1D)

    // 투명도 값은 Figma 변수 값을 그대로 따른다 (이름의 숫자와 다를 수 있음)
    val AlphaBlack60 = Color(0xB2000000)
    val AlphaGreen10 = Color(0x1A9BEA51)
    val AlphaGreen16 = Color(0x339BEA51)
    val AlphaTransparent = Color(0x1A000000)
    val AlphaWhite05 = Color(0x1AFFFFFF)
    val AlphaWhite08 = Color(0x14FFFFFF)
    val AlphaWhite12 = Color(0x1FFFFFFF)
}
