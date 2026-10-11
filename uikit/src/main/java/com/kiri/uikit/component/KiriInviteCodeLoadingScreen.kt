package com.kiri.uikit.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.uikit.color.KiriColor

// 가족코드(초대)를 발급하는 동안 보여주는 로딩 화면
// 온보딩 가족 공간 생성과 마이페이지 공용 초대 링크 새로 발급이 함께 쓴다
@Composable
fun KiriInviteCodeLoadingScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 코드 생성 중에는 뒤로가기 버튼을 비활성화한다
        KiriTopBar(onBackClick = {}, enabled = false)
        Spacer(Modifier.height(154.dp))
        KiriTitle(
            title = "가족코드를\n생성하고 있어요!",
            subtitle = "조금만 기다려주세요",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(44.dp))
        KiriLoadingIndicator()
    }
}

@Preview(showSystemUi = true)
@Composable
private fun KiriInviteCodeLoadingScreenPreview() {
    KiriInviteCodeLoadingScreen()
}
