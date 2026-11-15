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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.feature.auth.R
import com.kiri.uikit.KiriTypography
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
    val containerModifier = when {
        type == AuthAgreementRowType.All -> Modifier.background(KiriComponentColor.Input.BgFilled, shape)
        // 동의한 개별 약관 (Handoff 9817:14056): 배경 Input.BgDefault + 테두리 BorderFilled
        checked -> Modifier
            .background(KiriComponentColor.Input.BgDefault, shape)
            .border(1.dp, KiriComponentColor.Input.BorderFilled, shape)
        // 동의하지 않은 개별 약관 (Figma 8441:7328)
        else -> Modifier
            .background(KiriComponentColor.Input.BgDefault, shape)
            .border(1.dp, KiriComponentColor.Input.BorderDisabled, shape)
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
            // 체크 여부와 상관없이 같은 22dp 칸에 두어 체크해도 글자가 움직이지 않게 한다
            // 두 아이콘 모두 칸 가운데에 지름 22dp 원을 그린다 (빈 원은 테두리가, 체크 아이콘은 1dp 여백이 칸을 살짝 넘친다)
            Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(
                        if (checked) R.drawable.ic_auth_check_circle_active else R.drawable.ic_auth_check_circle_empty,
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .wrapContentSize(unbounded = true)
                        .then(if (checked) Modifier else Modifier.testTag("${testTag}_unchecked")),
                )
            }
            Spacer(
                Modifier.width(
                    when (type) {
                        AuthAgreementRowType.All -> 8.dp
                        AuthAgreementRowType.Required -> 12.dp
                    },
                ),
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                // Figma KIRI/Title/Medium. 24sp 줄 가운데에 글자를 두어 아이콘과 중심이 맞는다
                style = KiriTypography.TitleMedium,
                color = KiriComponentColor.Input.ContentFilled,
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
