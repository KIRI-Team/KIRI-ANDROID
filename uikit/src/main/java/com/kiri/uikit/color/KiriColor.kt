package com.kiri.uikit.color

import com.kiri.uikit.color.KiriPrimitiveColor as P

// Figma: 03 / Semantic Colors (KIRI/Color/Semantic)
object KiriColor {
    object Bg {
        val Base = P.Neutral950
        val Primary = P.Neutral900
        val Secondary = P.Neutral850
        val Tertiary = P.Neutral800
        val Elevated = P.Neutral700
        val Green = P.Green900
        val GreenSoft = P.AlphaGreen10
        val Transparent = P.AlphaTransparent
    }

    object Text {
        val Strong = P.Neutral50
        val Primary = P.Neutral200
        val Secondary = P.Neutral300
        val Tertiary = P.Neutral400
        val Disabled = P.Neutral500
        val Accent = P.Green500
        val Inverse = P.Neutral950
        val OnGreen = P.Neutral950
    }

    object Icon {
        val Strong = P.Neutral50
        val Primary = P.Neutral200
        val Secondary = P.Neutral400
        val Disabled = P.Neutral500
        val Accent = P.Green500
        val OnGreen = P.Neutral950
    }

    object Border {
        val Default = P.Neutral700
        val Strong = P.Neutral600
        val Subtle = P.Neutral800
        val Transparent = P.AlphaTransparent
    }

    val Divider = P.Neutral800
    val FocusRing = P.Green300

    object Primary {
        val Default = P.Green500
        val Pressed = P.Green600
        val Disabled = P.Green800
        val Container = P.AlphaGreen16
    }

    object State {
        val Pressed = P.AlphaWhite08
        val Selected = P.AlphaWhite12
        val Overlay = P.AlphaBlack60
        val GreenPressed = P.AlphaGreen16
    }

    object Status {
        val Error = P.Red500
        val ErrorBg = P.Red900
        val Info = P.Blue500
        val InfoBg = P.Blue900
        val Success = P.Green500
        val SuccessBg = P.AlphaGreen10
        val Warning = P.Yellow500
        val WarningBg = P.Yellow900
    }
}
