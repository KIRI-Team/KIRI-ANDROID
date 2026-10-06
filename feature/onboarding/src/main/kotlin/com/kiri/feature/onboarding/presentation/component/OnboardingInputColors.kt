package com.kiri.feature.onboarding.presentation.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kiri.uikit.color.KiriComponentColor

// KIRI/Input의 State 베리언트
internal enum class OnboardingInputState { Default, Filled, Focus, Error, Disabled }

// Disabled > Error > Focus > Filled > Default 순으로 우선한다
internal fun onboardingInputState(
    enabled: Boolean,
    isError: Boolean,
    isFocused: Boolean,
    isEmpty: Boolean,
): OnboardingInputState = when {
    !enabled -> OnboardingInputState.Disabled
    isError -> OnboardingInputState.Error
    isFocused -> OnboardingInputState.Focus
    !isEmpty -> OnboardingInputState.Filled
    else -> OnboardingInputState.Default
}

internal fun OnboardingInputState.background(): Color = when (this) {
    OnboardingInputState.Default -> KiriComponentColor.Input.BgDefault
    OnboardingInputState.Filled -> KiriComponentColor.Input.BgFilled
    OnboardingInputState.Focus -> KiriComponentColor.Input.BgFocus
    OnboardingInputState.Error -> KiriComponentColor.Input.BgError
    OnboardingInputState.Disabled -> KiriComponentColor.Input.BgDisabled
}

internal fun OnboardingInputState.borderColor(): Color = when (this) {
    OnboardingInputState.Default -> KiriComponentColor.Input.BorderDefault
    OnboardingInputState.Filled -> KiriComponentColor.Input.BorderFilled
    OnboardingInputState.Focus -> KiriComponentColor.Input.BorderFocus
    OnboardingInputState.Error -> KiriComponentColor.Input.BorderError
    OnboardingInputState.Disabled -> KiriComponentColor.Input.BorderDisabled
}

// Focus·Error는 2dp, 나머지는 1dp 테두리
internal fun OnboardingInputState.borderWidth(): Dp = when (this) {
    OnboardingInputState.Focus, OnboardingInputState.Error -> 2.dp
    else -> 1.dp
}

internal fun OnboardingInputState.contentColor(): Color = when (this) {
    OnboardingInputState.Default -> KiriComponentColor.Input.ContentDefault
    OnboardingInputState.Filled -> KiriComponentColor.Input.ContentFilled
    OnboardingInputState.Focus -> KiriComponentColor.Input.ContentFocus
    OnboardingInputState.Error -> KiriComponentColor.Input.ContentError
    OnboardingInputState.Disabled -> KiriComponentColor.Input.ContentDisabled
}

internal fun OnboardingInputState.placeholderColor(): Color = when (this) {
    OnboardingInputState.Focus -> KiriComponentColor.Input.PlaceholderFocus
    OnboardingInputState.Error -> KiriComponentColor.Input.PlaceholderError
    OnboardingInputState.Disabled -> KiriComponentColor.Input.PlaceholderDisabled
    else -> KiriComponentColor.Input.PlaceholderDefault
}
