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
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.mypage.FamilySettingViewModel
import com.kiri.feature.mypage.toProfile
import com.kiri.feature.mypage.model.common.MypageSection
import com.kiri.feature.mypage.presentation.common.MypageMenuGroup
import com.kiri.feature.mypage.presentation.main.component.MypageProfileRow
import com.kiri.uikit.color.KiriColor

// 프로필(닉네임, 가족 이름, 역할)은 가족 정보 API 로 채운다. 이름을 바꾸고 돌아오면 다시 불러온다
@Composable
fun MypageRoute(
    onMenuClick: (MypageMenu) -> Unit,
    onProfileEditClick: () -> Unit,
    viewModel: FamilySettingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    MypageScreen(onMenuClick = onMenuClick, profile = state.toProfile(), onProfileEditClick = onProfileEditClick)
}

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
