package com.kiri.feature.mypage.presentation.preference

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kiri.feature.mypage.model.common.MypageMenu
import com.kiri.feature.mypage.presentation.common.MypagePlaceholderScreen

// 내 취향 설정
// TODO: 디자인이 정해지면 화면 구현 (현재는 자리표시자)
@Composable
fun MyPreferenceScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MypagePlaceholderScreen(
        title = MypageMenu.MY_PREFERENCE.title,
        onBackClick = onBackClick,
        testTag = "my_preference_screen",
        modifier = modifier,
    )
}

@Preview(showSystemUi = true)
@Composable
private fun MyPreferenceScreenPreview() {
    MyPreferenceScreen(onBackClick = {})
}
