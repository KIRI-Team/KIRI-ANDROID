package com.kiri.feature.onboarding.presentation.component

import com.kiri.uikit.color.KiriComponentColor
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingButtonColorsTest {

    private val accent = KiriComponentColor.Button.FilledAccent
    private val neutral = KiriComponentColor.Button.FilledNeutral

    @Test
    fun 기본_상태에서는_Default_색상을_쓴다() {
        assertEquals(
            OnboardingButtonColors(accent.BgDefault, accent.ContentDefault),
            onboardingButtonColors(OnboardingButtonTone.Accent, enabled = true, pressed = false),
        )
        assertEquals(
            OnboardingButtonColors(neutral.BgDefault, neutral.ContentDefault),
            onboardingButtonColors(OnboardingButtonTone.Neutral, enabled = true, pressed = false),
        )
    }

    @Test
    fun 눌린_상태에서는_Pressed_색상을_쓴다() {
        assertEquals(
            OnboardingButtonColors(accent.BgPressed, accent.ContentPressed),
            onboardingButtonColors(OnboardingButtonTone.Accent, enabled = true, pressed = true),
        )
        assertEquals(
            OnboardingButtonColors(neutral.BgPressed, neutral.ContentPressed),
            onboardingButtonColors(OnboardingButtonTone.Neutral, enabled = true, pressed = true),
        )
    }

    @Test
    fun 비활성_상태는_눌림보다_우선한다() {
        assertEquals(
            OnboardingButtonColors(accent.BgDisabled, accent.ContentDisabled),
            onboardingButtonColors(OnboardingButtonTone.Accent, enabled = false, pressed = true),
        )
        assertEquals(
            OnboardingButtonColors(neutral.BgDisabled, neutral.ContentDisabled),
            onboardingButtonColors(OnboardingButtonTone.Neutral, enabled = false, pressed = true),
        )
    }
}
