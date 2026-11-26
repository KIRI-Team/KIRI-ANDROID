package com.kiri.feature.auth.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.kiri.feature.auth.presentation.intro.LoginIntroMotion
import com.kiri.uikit.color.KiriColor

// 앱 시작 스플래시. 로그인 화면과 같은 애니메이션을 로그인 버튼이 나타나기 직전(SPLASH_END_MS)까지 재생한다
// 자동 로그인이어도 여기까지는 보여주고, 로그인이 필요하면 LoginScreen(continueFromSplash = true)이 이어서 버튼을 띄운다
// play 가 false 면 멈춘 모습으로 바로 보여준다 (재생을 마친 뒤 화면이 다시 만들어졌을 때)
// content 는 재생을 마친 뒤에도 로그인 상태를 확인 중이거나 실패했을 때 아래쪽에 띄울 내용이다
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    play: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val time = remember { Animatable(if (play) 0f else LoginIntroMotion.SPLASH_END_MS) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        if (time.value < LoginIntroMotion.SPLASH_END_MS) {
            time.animateTo(
                targetValue = LoginIntroMotion.SPLASH_END_MS,
                animationSpec = tween(durationMillis = LoginIntroMotion.SPLASH_END_MS.toInt(), easing = LinearEasing),
            )
        }
        currentOnFinished()
    }

    Box(
        modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .testTag("splash_screen"),
    ) {
        LoginIntroScene(now = { time.value }, modifier = Modifier.align(Alignment.Center))
        content()
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    SplashScreen(onFinished = {}, play = false)
}
