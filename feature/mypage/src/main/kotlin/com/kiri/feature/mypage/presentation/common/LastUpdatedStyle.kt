package com.kiri.feature.mypage.presentation.common

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.uikit.color.KiriColor

// 약관·라이선스 목록 오른쪽 아래 "최종 업데이트" 글자 (KIRI/Body/Small)
internal val lastUpdatedStyle = TextStyle(
    color = KiriColor.Text.Tertiary,
    fontSize = 12.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 18.sp,
    letterSpacing = (-0.04).em,
)
