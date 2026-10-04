package com.kiri.feature.onboarding.presentation.component

import androidx.compose.ui.graphics.Color
import com.kiri.uikit.color.KiriComponentColor

// KIRI/Card: Default / Pressed 상태별 배경색
internal fun onboardingCardBackground(pressed: Boolean): Color =
    if (pressed) KiriComponentColor.Card.BgPressed else KiriComponentColor.Card.BgDefault
