package com.kiri.feature.auth.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.feature.auth.R
import com.kiri.feature.auth.model.LoginProvider

// 각 플랫폼 브랜드 가이드 색상이라 KIRI 색상 토큰이 아닌 고정값을 쓴다
private data class LoginButtonStyle(
    val text: String,
    val iconRes: Int,
    val containerColor: Color,
    val contentColor: Color,
    val border: BorderStroke?,
)

private fun LoginProvider.style(): LoginButtonStyle = when (this) {
    LoginProvider.KAKAO -> LoginButtonStyle(
        text = "카카오 로그인",
        iconRes = R.drawable.ic_auth_kakao,
        containerColor = Color(0xFFFEE500),
        contentColor = Color(0xFF191919),
        border = null,
    )
    LoginProvider.GOOGLE -> LoginButtonStyle(
        text = "Google로 로그인",
        iconRes = R.drawable.ic_auth_google,
        containerColor = Color(0xFFFFFFFF),
        contentColor = Color(0xFF2B2B2B),
        border = BorderStroke(1.dp, Color(0xFFD9D9D9)),
    )
}

@Composable
fun AuthLoginButton(
    provider: LoginProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = provider.style()
    val shape = RoundedCornerShape(30.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(style.containerColor)
            .then(if (style.border != null) Modifier.border(style.border, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("auth_login_button_${provider.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(style.iconRes),
                contentDescription = null,
            )
        }
        // Figma: 아이콘을 뺀 나머지 영역의 가운데에 문구를 둔다
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = style.text,
                style = TextStyle(
                    color = style.contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 1.45.em,
                    letterSpacing = (-0.01).em,
                ),
            )
        }
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun AuthLoginButtonPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AuthLoginButton(provider = LoginProvider.KAKAO, onClick = {})
        AuthLoginButton(provider = LoginProvider.GOOGLE, onClick = {})
    }
}
