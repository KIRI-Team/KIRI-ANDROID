package com.kiri.feature.auth.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.kiri.uikit.color.KiriComponentColor

// KIRI/Agreement Row: Type=All(전체동의) / Type=Required(개별 약관)
enum class AuthAgreementRowType { All, Required }

/**
 * 오른쪽 화살표를 제외한 행 전체를 누르면 동의가 바뀐다.
 * @param onDetailClick 개별 약관에서 오른쪽 화살표를 누르면 세부 내용을 연다. 전체동의 행은 null (화살표도 동의 영역에 포함).
 */
@Composable
fun AuthAgreementRow(
    title: String,
    type: AuthAgreementRowType,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    onDetailClick: (() -> Unit)? = null,
    testTag: String = "auth_agreement_row",
) {
    val shape = RoundedCornerShape(8.dp)
    val containerModifier = when (type) {
        AuthAgreementRowType.All -> Modifier.background(KiriComponentColor.Input.BgFilled, shape)
        AuthAgreementRowType.Required -> Modifier.border(1.dp, KiriComponentColor.Input.BorderFilled, shape)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .then(containerModifier)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onCheckedChange() })
                .padding(start = 24.dp, end = if (onDetailClick == null) 24.dp else 0.dp)
                .testTag("${testTag}_checkbox"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(
                    if (checked) R.drawable.ic_auth_check_circle_active else R.drawable.ic_auth_check_circle_disabled,
                ),
                contentDescription = null,
            )
            Spacer(
                Modifier.width(
                    when (type) {
                        AuthAgreementRowType.All -> 8.dp
                        AuthAgreementRowType.Required -> 6.dp
                    },
                ),
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    color = KiriComponentColor.Input.ContentFilled,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.04).em,
                ),
            )
            if (onDetailClick == null) {
                Image(painter = painterResource(R.drawable.ic_auth_chevron_right), contentDescription = null)
            }
        }
        if (onDetailClick != null) {
            // 오른쪽 여백까지 누를 수 있게 해서 터치 영역을 넓힌다
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(onClick = onDetailClick)
                    .padding(end = 24.dp)
                    .testTag("${testTag}_detail"),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_auth_chevron_right),
                    contentDescription = "약관 상세 보기",
                )
            }
        }
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun AuthAgreementRowPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AuthAgreementRow(title = "전체동의", type = AuthAgreementRowType.All, checked = false, onCheckedChange = {})
        AuthAgreementRow(
            title = "[필수] 서비스 이용약관",
            type = AuthAgreementRowType.Required,
            checked = true,
            onCheckedChange = {},
            onDetailClick = {},
        )
    }
}
