package com.kiri.feature.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.feature.auth.R
import com.kiri.feature.auth.model.LoginProvider
import com.kiri.feature.auth.presentation.component.AuthLoginButton
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriTopBar

@Composable
fun LoginScreen(
    onLoginClick: (LoginProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .systemBarsPadding()
            .testTag("login_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 첫 화면이라 돌아갈 곳이 없어 뒤로가기 버튼은 비활성으로 둔다
        KiriTopBar(onBackClick = {}, enabled = false)
        Spacer(Modifier.height(150.dp))
        Image(
            painter = painterResource(R.drawable.ic_auth_logo),
            contentDescription = null,
            // Figma에서 로고는 가운데보다 2dp 왼쪽에 놓여 있다
            modifier = Modifier.offset(x = (-2).dp),
        )
        Spacer(Modifier.height(24.dp))
        Image(
            painter = painterResource(R.drawable.ic_auth_wordmark),
            contentDescription = "KIRI",
        )
        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AuthLoginButton(provider = LoginProvider.KAKAO, onClick = { onLoginClick(LoginProvider.KAKAO) })
            AuthLoginButton(provider = LoginProvider.GOOGLE, onClick = { onLoginClick(LoginProvider.GOOGLE) })
        }
        Spacer(Modifier.height(97.dp))
    }
}

@Preview(showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    LoginScreen(onLoginClick = {})
}
