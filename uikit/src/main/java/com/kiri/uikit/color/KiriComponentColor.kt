package com.kiri.uikit.color

import com.kiri.uikit.color.KiriPrimitiveColor as P

// Figma: KIRI/Color/Component — 컴포넌트별 상태 색상
object KiriComponentColor {
    object Button {
        object FilledAccent {
            val BgDefault = P.Green500
            val BgPressed = P.Green600
            val BgDisabled = P.Neutral800
            val ContentDefault = P.Neutral950
            val ContentPressed = P.Neutral950
            val ContentDisabled = P.Neutral500
        }

        object FilledNeutral {
            val BgDefault = P.Neutral700
            val BgPressed = P.Neutral800
            val BgDisabled = P.Neutral850
            val ContentDefault = P.Neutral200
            val ContentPressed = P.Neutral200
            val ContentDisabled = P.Neutral500
        }

        object Ghost {
            val BgDefault = P.AlphaTransparent
            val BgPressed = P.AlphaGreen16
            val BgDisabled = P.AlphaTransparent
            val ContentDefault = P.Green500
            val ContentPressed = P.Green600
            val ContentDisabled = P.Neutral500
        }
    }

    object Card {
        val BgDefault = P.Neutral700
        val BgPressed = P.Neutral800
        val BgSelected = P.AlphaGreen16
        val BgDisabled = P.Neutral900
        val BorderDefault = P.Neutral800
        val BorderPressed = P.Neutral700
        val BorderSelected = P.Green500
        val BorderDisabled = P.Neutral800
    }

    object Chip {
        val BgDefault = P.Neutral800
        val BgPressed = P.Neutral700
        val BgSelected = P.AlphaGreen16
        val BgDisabled = P.Neutral900
        val BorderDefault = P.Neutral700
        val BorderPressed = P.Neutral600
        val BorderSelected = P.Green500
        val BorderDisabled = P.Neutral800
        val ContentDefault = P.Neutral300
        val ContentPressed = P.Neutral200
        val ContentSelected = P.Green500
        val ContentDisabled = P.Neutral500
    }

    object Input {
        val BgDefault = P.Neutral850
        val BgFocus = P.Neutral850
        val BgFilled = P.Neutral850
        val BgError = P.Neutral850
        val BgDisabled = P.Neutral900
        val BorderDefault = P.Neutral700
        val BorderFocus = P.Green500
        val BorderFilled = P.Neutral700
        val BorderError = P.Red500
        val BorderDisabled = P.Neutral800
        val ContentDefault = P.Neutral200
        val ContentFocus = P.Neutral200
        val ContentFilled = P.Neutral200
        val ContentError = P.Neutral200
        val ContentDisabled = P.Neutral500
        val PlaceholderDefault = P.Neutral300
        val PlaceholderFocus = P.Neutral300
        val PlaceholderError = P.Neutral300
        val PlaceholderDisabled = P.Neutral500
        val HelperDefault = P.Neutral400
        val HelperFocus = P.Neutral400
        val HelperFilled = P.Neutral400
        val HelperError = P.Red500
        val HelperDisabled = P.Neutral500
    }

    object Navigation {
        val BarBg = P.Neutral700
        val BarBorder = P.Neutral600
        val ItemBgDefault = P.AlphaTransparent
        val ItemBgPressed = P.AlphaWhite08
        val ItemBgActive = P.Neutral950
        val ItemBgDisabled = P.AlphaTransparent
        val ItemContentDefault = P.Neutral400
        val ItemContentPressed = P.Neutral200
        val ItemContentActive = P.Neutral50
        val ItemContentDisabled = P.Neutral500
    }

    object Tab {
        val ContentDefault = P.Neutral400
        val ContentPressed = P.Neutral200
        val ContentActive = P.Neutral50
        val ContentDisabled = P.Neutral500
        val IndicatorDefault = P.AlphaTransparent
        val IndicatorPressed = P.Neutral600
        val IndicatorActive = P.Neutral50
        val IndicatorDisabled = P.AlphaTransparent
    }
}
