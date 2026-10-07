package com.kiri.feature.onboarding.presentation.component

import androidx.compose.ui.unit.dp
import com.kiri.uikit.color.KiriComponentColor
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingInputColorsTest {

    @Test
    fun 비어있고_포커스가_없으면_Default_상태다() {
        assertEquals(OnboardingInputState.Default, state(isEmpty = true))
    }

    @Test
    fun 값이_있고_포커스가_없으면_Filled_상태다() {
        assertEquals(OnboardingInputState.Filled, state(isEmpty = false))
    }

    @Test
    fun 포커스가_있으면_값과_관계없이_Focus_상태다() {
        assertEquals(OnboardingInputState.Focus, state(isFocused = true, isEmpty = true))
        assertEquals(OnboardingInputState.Focus, state(isFocused = true, isEmpty = false))
    }

    @Test
    fun 오류는_포커스보다_우선한다() {
        assertEquals(OnboardingInputState.Error, state(isError = true, isFocused = true))
    }

    @Test
    fun 비활성화는_오류보다_우선한다() {
        assertEquals(OnboardingInputState.Disabled, state(enabled = false, isError = true, isFocused = true))
    }

    @Test
    fun Focus와_Error만_2dp_테두리를_쓴다() {
        assertEquals(2.dp, OnboardingInputState.Focus.borderWidth())
        assertEquals(2.dp, OnboardingInputState.Error.borderWidth())
        assertEquals(1.dp, OnboardingInputState.Default.borderWidth())
        assertEquals(1.dp, OnboardingInputState.Filled.borderWidth())
        assertEquals(1.dp, OnboardingInputState.Disabled.borderWidth())
    }

    @Test
    fun 상태별_테두리_색을_쓴다() {
        assertEquals(KiriComponentColor.Input.BorderDefault, OnboardingInputState.Default.borderColor())
        assertEquals(KiriComponentColor.Input.BorderFocus, OnboardingInputState.Focus.borderColor())
        assertEquals(KiriComponentColor.Input.BorderError, OnboardingInputState.Error.borderColor())
        assertEquals(KiriComponentColor.Input.BorderDisabled, OnboardingInputState.Disabled.borderColor())
    }

    @Test
    fun Disabled만_다른_배경과_글자색을_쓴다() {
        assertEquals(KiriComponentColor.Input.BgDisabled, OnboardingInputState.Disabled.background())
        assertEquals(KiriComponentColor.Input.ContentDisabled, OnboardingInputState.Disabled.contentColor())
        assertEquals(KiriComponentColor.Input.PlaceholderDisabled, OnboardingInputState.Disabled.placeholderColor())
        assertEquals(KiriComponentColor.Input.BgDefault, OnboardingInputState.Default.background())
    }

    private fun state(
        enabled: Boolean = true,
        isError: Boolean = false,
        isFocused: Boolean = false,
        isEmpty: Boolean = true,
    ) = onboardingInputState(enabled = enabled, isError = isError, isFocused = isFocused, isEmpty = isEmpty)
}
