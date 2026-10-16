package com.kiri.feature.record.presentation.sheet

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriButtonTone
import com.kiri.uikit.component.KiriPretendard

// 기록 탭 시트들이 같이 쓰는 틀. 위쪽 모서리 30, 손잡이 없음 (Figma sheet / Rectangle 14)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordModalSheet(
    onDismiss: () -> Unit,
    testTag: String,
    containerColor: Color = KiriColor.Bg.Secondary,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = containerColor,
        dragHandle = null,
        modifier = Modifier.testTag(testTag),
        content = content,
    )
}

@Composable
internal fun RecordSheetTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 18.dp),
        color = KiriColor.Text.Strong,
        style = RecordSheetTitleStyle,
    )
}

// KIRI/Bottom Action (Layout=Single): 좌우 18, 아래 30 여백 안에 Neutral 버튼 하나
@Composable
internal fun RecordSheetBottomAction(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    KiriButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, bottom = 30.dp),
        enabled = enabled,
        text = text,
        tone = KiriButtonTone.Neutral,
    )
}

// 프로젝트 폰트에 SemiBold 파일이 없어 Medium 을 굵게 합성해서 그린다
private val RecordSheetTitleStyle = TextStyle(
    fontFamily = KiriPretendard,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.54).sp,
)
