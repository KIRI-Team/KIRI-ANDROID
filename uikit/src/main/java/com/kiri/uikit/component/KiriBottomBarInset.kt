package com.kiri.uikit.component

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 탭 화면 콘텐츠 위에 떠 있는 바텀 바가 가리는 높이.
 * 탭 화면은 콘텐츠를 바 뒤까지 그리므로, 스크롤 영역 끝에 이 값만큼 여백을 더해 마지막 항목이 바에 가리지 않게 한다.
 */
val LocalKiriBottomBarInset = staticCompositionLocalOf { 0.dp }
