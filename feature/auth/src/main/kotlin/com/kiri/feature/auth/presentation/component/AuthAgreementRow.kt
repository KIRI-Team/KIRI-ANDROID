package com.kiri.feature.auth.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.color.KiriComponentColor

// KIRI/Agreement Row: Type=All(전체동의) / Type=Required(개별 약관)
enum class AuthAgreementRowType { All, Required }

/**
 * @param onDetailClick 개별 약관에서 문구/화살표를 누르면 상세(외부 링크)로 이동한다. 전체동의 행은 null.
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
            // 전체동의는 행 전체가, 개별 약관은 문구 영역이 상세 이동을 담당한다
            .then(if (type == AuthAgreementRowType.All) Modifier.clickable(onClick = onCheckedChange) else Modifier)
            .padding(horizontal = 24.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AgreementCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            uncheckedBorderColor = when (type) {
                AuthAgreementRowType.All -> KiriColor.Border.Strong
                AuthAgreementRowType.Required -> KiriColor.Border.Transparent
            },
            modifier = Modifier.testTag("${testTag}_checkbox"),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(if (onDetailClick != null) Modifier.clickable(onClick = onDetailClick) else Modifier)
                .testTag("${testTag}_detail"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 2.dp),
                style = TextStyle(
                    color = KiriComponentColor.Input.ContentFilled,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.04).em,
                ),
            )
            Image(
                painter = painterResource(R.drawable.ic_auth_chevron_right),
                contentDescription = if (onDetailClick != null) "약관 상세 보기" else null,
            )
        }
    }
}

@Composable
private fun AgreementCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    uncheckedBorderColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onCheckedChange() }),
        contentAlignment = Alignment.Center,
    ) {
        val boxShape = RoundedCornerShape(4.dp)
        if (checked) {
            // TODO: Figma에 체크된 상태가 없어 임시 스타일 (Primary 배경 + 체크 표시)
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(KiriColor.Primary.Default, boxShape),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(size.width * 0.24f, size.height * 0.52f)
                        lineTo(size.width * 0.43f, size.height * 0.70f)
                        lineTo(size.width * 0.77f, size.height * 0.33f)
                    }
                    drawPath(
                        path = path,
                        color = KiriColor.Icon.OnGreen,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(1.dp, uncheckedBorderColor, boxShape),
            )
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
