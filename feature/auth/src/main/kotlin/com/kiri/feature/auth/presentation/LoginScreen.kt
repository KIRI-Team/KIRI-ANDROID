package com.kiri.feature.auth.presentation

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.feature.auth.R
import com.kiri.feature.auth.model.LoginProvider
import com.kiri.feature.auth.presentation.component.AuthLoginButton
import com.kiri.feature.auth.presentation.intro.IntroPiece
import com.kiri.feature.auth.presentation.intro.LoginIntroMotion
import com.kiri.uikit.color.KiriColor

private val PIECE_IMAGES = listOf(
    R.drawable.img_auth_intro_piece_1,
    R.drawable.img_auth_intro_piece_2,
    R.drawable.img_auth_intro_piece_3,
    R.drawable.img_auth_intro_piece_4,
)

// 로그인 첫 화면. 들어올 때 Figma 스플래시 애니메이션을 한 번 재생하고, 끝나갈 때 로그인 버튼이 나타난다
// playIntro 가 false 면 애니메이션 없이 마지막 모습으로 바로 보여준다 (다른 단계에서 돌아왔을 때, 미리보기)
@Composable
fun LoginScreen(
    onLoginClick: (LoginProvider) -> Unit,
    modifier: Modifier = Modifier,
    playIntro: Boolean = true,
    onIntroFinished: () -> Unit = {},
) {
    val time = remember { Animatable(if (playIntro) 0f else LoginIntroMotion.END_MS) }
    val currentOnIntroFinished by rememberUpdatedState(onIntroFinished)
    LaunchedEffect(Unit) {
        if (time.value < LoginIntroMotion.END_MS) {
            time.animateTo(
                targetValue = LoginIntroMotion.END_MS,
                animationSpec = tween(durationMillis = LoginIntroMotion.END_MS.toInt(), easing = LinearEasing),
            )
        }
        currentOnIntroFinished()
    }
    // 매 프레임 바뀌는 값은 graphicsLayer 안에서 읽어 화면 전체를 다시 구성하지 않게 한다
    val now: () -> Float = { time.value }
    // 버튼이 보이기 시작하기 전에는 눌리지 않게 한다
    val buttonsEnabled by remember { derivedStateOf { LoginIntroMotion.buttonsAlpha.valueAt(time.value) > 0f } }

    Box(
        modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .testTag("login_screen"),
    ) {
        // Figma 화면(390×844)을 가운데에 맞춰 놓고 그 좌표 그대로 배치한다
        Box(
            Modifier
                .align(Alignment.Center)
                .requiredSize(LoginIntroMotion.DESIGN_WIDTH.dp, LoginIntroMotion.DESIGN_HEIGHT.dp),
        ) {
            LoginIntroMotion.pieces.forEachIndexed { index, piece ->
                IntroPieceImage(piece = piece, image = PIECE_IMAGES[index], now = now)
            }
            Image(
                painter = painterResource(R.drawable.img_auth_intro_wordmark),
                contentDescription = "KIRI",
                modifier = Modifier
                    .centeredAt(LoginIntroMotion.WORDMARK_CENTER_X, LoginIntroMotion.WORDMARK_CENTER_Y, 275.934f, 99.531f)
                    .graphicsLayer {
                        val t = now()
                        alpha = LoginIntroMotion.wordmarkAlpha.valueAt(t)
                        LoginIntroMotion.wordmarkScale.valueAt(t).let { scaleX = it; scaleY = it }
                    }
                    .testTag("login_wordmark"),
            )
            Image(
                painter = painterResource(R.drawable.img_auth_intro_character),
                contentDescription = null,
                modifier = Modifier
                    .centeredAt(LoginIntroMotion.CHARACTER_CENTER_X, LoginIntroMotion.CHARACTER_CENTER_Y, 56f, 56f)
                    .graphicsLayer {
                        val t = now()
                        translationY = LoginIntroMotion.characterTranslateY.valueAt(t).dp.toPx()
                        alpha = LoginIntroMotion.characterAlpha.valueAt(t)
                        rotationZ = LoginIntroMotion.characterRotation.valueAt(t)
                        LoginIntroMotion.characterScale.valueAt(t).let { scaleX = it; scaleY = it }
                    },
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .graphicsLayer { alpha = LoginIntroMotion.buttonsAlpha.valueAt(now()) },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AuthLoginButton(
                provider = LoginProvider.KAKAO,
                onClick = { onLoginClick(LoginProvider.KAKAO) },
                enabled = buttonsEnabled,
            )
            AuthLoginButton(
                provider = LoginProvider.GOOGLE,
                onClick = { onLoginClick(LoginProvider.GOOGLE) },
                enabled = buttonsEnabled,
            )
            // Figma: 구글 버튼 아래로 홈 인디케이터까지 97dp
            Spacer(Modifier.height(97.dp))
        }
    }
}

// 회색 글자 조각. 초록으로 바뀌며 커지고 움직이다 사라진다
@Composable
private fun IntroPieceImage(
    piece: IntroPiece,
    @DrawableRes image: Int,
    now: () -> Float,
) {
    // 색은 이미지 그리기에 필요해 구성 단계에서 읽는다 (조각 4개만 다시 그린다)
    val green = piece.green.valueAt(now())
    Image(
        painter = painterResource(image),
        contentDescription = null,
        colorFilter = ColorFilter.tint(lerp(KiriColor.Bg.Tertiary, KiriColor.Primary.Default, green)),
        modifier = Modifier
            .centeredAt(piece.centerX, piece.centerY, piece.width, piece.height)
            .graphicsLayer {
                val t = now()
                translationX = piece.translateX.valueAt(t).dp.toPx()
                translationY = piece.translateY.valueAt(t).dp.toPx()
                rotationZ = piece.rotation
                piece.scale.valueAt(t).let { scaleX = it; scaleY = it }
                alpha = piece.alpha.valueAt(t)
            },
    )
}

// Figma 좌표(가운데 점과 크기, dp)로 놓는다
private fun Modifier.centeredAt(centerX: Float, centerY: Float, width: Float, height: Float): Modifier =
    offset(x = (centerX - width / 2).dp, y = (centerY - height / 2).dp)

@Preview(showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    LoginScreen(onLoginClick = {}, playIntro = false)
}
