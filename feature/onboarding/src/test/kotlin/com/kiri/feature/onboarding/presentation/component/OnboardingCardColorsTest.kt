package com.kiri.feature.onboarding.presentation.component

import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.color.KiriComponentColor
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingCardColorsTest {

    @Test
    fun 기본_상태에서는_Bg_Secondary_배경을_쓴다() {
        // Figma 가족공간 선택 카드 (Handoff 9817:14183)
        assertEquals(KiriColor.Bg.Secondary, onboardingCardBackground(pressed = false))
    }

    @Test
    fun 눌린_상태에서는_Pressed_배경을_쓴다() {
        assertEquals(KiriComponentColor.Card.BgPressed, onboardingCardBackground(pressed = true))
    }
}
