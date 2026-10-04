package com.kiri.feature.onboarding.presentation.component

import com.kiri.uikit.color.KiriComponentColor
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingCardColorsTest {

    @Test
    fun 기본_상태에서는_Default_배경을_쓴다() {
        assertEquals(KiriComponentColor.Card.BgDefault, onboardingCardBackground(pressed = false))
    }

    @Test
    fun 눌린_상태에서는_Pressed_배경을_쓴다() {
        assertEquals(KiriComponentColor.Card.BgPressed, onboardingCardBackground(pressed = true))
    }
}
