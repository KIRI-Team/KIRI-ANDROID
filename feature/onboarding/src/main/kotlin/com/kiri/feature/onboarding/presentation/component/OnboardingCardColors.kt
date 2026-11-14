package com.kiri.feature.onboarding.presentation.component

import androidx.compose.ui.graphics.Color
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.color.KiriComponentColor

// 가족공간 선택 카드(Figma KIRI/Card/Action Row) 배경: 기본 Bg.Secondary, 눌림 Card.BgPressed
internal fun onboardingCardBackground(pressed: Boolean): Color =
    if (pressed) KiriComponentColor.Card.BgPressed else KiriColor.Bg.Secondary
