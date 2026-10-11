package com.kiri.feature.mypage.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.color.KiriComponentColor

// 가운데 뜨는 팝업 (로그아웃 확인, 오픈 라이선스 세부 내용)
// 제목·안내 문구 아래에 content 를 두고, 맨 아래에 buttons 를 가로로 나란히 둔다. 바깥 탭·뒤로가기로 닫힌다
@Composable
fun MypageDialog(
    title: String,
    onDismiss: () -> Unit,
    buttons: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    testTag: String = "mypage_dialog",
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismiss,
        // Figma 팝업은 화면 양옆에 23dp 만 남기므로 기본 너비 제한을 끈다
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 23.dp)
                .fillMaxWidth()
                .testTag(testTag)
                .clip(RoundedCornerShape(16.dp))
                .background(KiriColor.Bg.Secondary)
                .padding(horizontal = 22.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(42.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = KiriColor.Text.Strong,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 28.sp,
                            letterSpacing = (-0.04).em,
                        ),
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = TextStyle(
                                color = KiriColor.Text.Tertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 18.sp,
                                letterSpacing = (-0.04).em,
                            ),
                        )
                    }
                }
                content()
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                content = buttons,
            )
        }
    }
}

enum class MypageDialogButtonTone { Neutral, Accent }

// KIRI/Button Layout=Sheet. 팝업 안에서 나란히 놓이는 54dp 높이 버튼
@Composable
fun RowScope.MypageDialogButton(
    text: String,
    onClick: () -> Unit,
    tone: MypageDialogButtonTone,
    testTag: String,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val (background, content) = when (tone) {
        // Sheet 레이아웃의 Neutral 은 일반 버튼(Neutral700)보다 어두운 바탕을 쓴다
        MypageDialogButtonTone.Neutral ->
            (if (pressed) KiriComponentColor.Button.FilledNeutral.BgPressed else KiriColor.Bg.Primary) to
                KiriComponentColor.Button.FilledNeutral.ContentDefault
        MypageDialogButtonTone.Accent -> with(KiriComponentColor.Button.FilledAccent) {
            (if (pressed) BgPressed else BgDefault) to ContentDefault
        }
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = content,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp,
                letterSpacing = (-0.04).em,
            ),
        )
    }
}

@Preview
@Composable
private fun MypageDialogPreview() {
    MypageDialog(
        title = "정말로 로그아웃 하시겠어요?",
        subtitle = "다시 로그인하면 우리 가족의 기록을 이어갈 수 있어요.",
        onDismiss = {},
        buttons = {
            MypageDialogButton(text = "취소", onClick = {}, tone = MypageDialogButtonTone.Neutral, testTag = "cancel")
            MypageDialogButton(text = "확인", onClick = {}, tone = MypageDialogButtonTone.Accent, testTag = "confirm")
        },
    )
}
