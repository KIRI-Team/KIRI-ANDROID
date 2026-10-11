package com.kiri.feature.mypage.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.feature.mypage.model.common.MypageMenu
import com.kiri.feature.mypage.model.common.MypageProfile
import com.kiri.feature.mypage.model.common.MypageSection
import com.kiri.feature.mypage.presentation.common.MypageMenuGroup
import com.kiri.feature.mypage.presentation.main.component.MypageProfileRow
import com.kiri.uikit.color.KiriColor

// 시스템 바 여백은 메인 화면의 Scaffold가 처리한다
@Composable
fun MypageScreen(
    onMenuClick: (MypageMenu) -> Unit,
    modifier: Modifier = Modifier,
    profile: MypageProfile = MypageProfile.Sample,
    onProfileEditClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .testTag("mypage_screen"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "설정",
                style = TextStyle(
                    color = KiriColor.Text.Strong,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.04).em,
                ),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            MypageProfileRow(profile = profile, onEditClick = onProfileEditClick)
            MypageSection.entries.forEach { section ->
                Spacer(Modifier.height(48.dp))
                MypageMenuGroup(
                    title = section.title,
                    menus = MypageMenu.entries.filter { it.section == section },
                    onMenuClick = onMenuClick,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun MypageScreenPreview() {
    MypageScreen(onMenuClick = {})
}
