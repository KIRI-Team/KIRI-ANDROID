package com.kiri.uikit.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.uikit.R
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.color.KiriComponentColor

// KIRI/Invite Code 카드에 들어가는 값의 종류. 코드는 크게, 링크는 한 단계 작게 보여준다
enum class KiriInviteCodeValue { Code, Link }

// KIRI/Invite Code: 제목(링크코드·초대링크)과 값, 복사 아이콘. 카드 전체를 누르면 복사한다
@Composable
fun KiriInviteCodeCard(
    label: String,
    value: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
    valueType: KiriInviteCodeValue = KiriInviteCodeValue.Code,
    testTag: String = "kiri_invite_code_card",
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val valueStyle = when (valueType) {
        KiriInviteCodeValue.Code -> TextStyle(
            color = KiriColor.Text.Strong,
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 36.sp,
            letterSpacing = (-0.02).em,
        )
        KiriInviteCodeValue.Link -> TextStyle(
            color = KiriColor.Text.Strong,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 28.sp,
            letterSpacing = (-0.04).em,
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (pressed) KiriComponentColor.Card.BgPressed else KiriComponentColor.Card.BgDefault)
            // 눌림 피드백은 Pressed 배경색으로 표현해서 기본 리플은 끈다
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = "$label 복사",
                role = Role.Button,
                onClick = onCopyClick,
            )
            .padding(10.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = KiriColor.Text.Tertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.sp,
                    letterSpacing = (-0.04).em,
                ),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (valueType == KiriInviteCodeValue.Code) 4.dp else 8.dp),
            ) {
                Text(
                    text = value,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = valueStyle,
                )
                Image(painter = painterResource(R.drawable.kiri_copy), contentDescription = null)
            }
        }
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun KiriInviteCodeCardPreview() {
    KiriInviteCodeCard(label = "링크코드", value = "NWTKYJ", onCopyClick = {})
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun KiriInviteCodeCardLinkPreview() {
    KiriInviteCodeCard(
        label = "초대링크",
        value = "kiri.app/invite/••••••",
        onCopyClick = {},
        valueType = KiriInviteCodeValue.Link,
    )
}
