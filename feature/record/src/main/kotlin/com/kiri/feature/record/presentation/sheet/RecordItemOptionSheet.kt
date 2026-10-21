package com.kiri.feature.record.presentation.sheet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kiri.feature.record.R
import com.kiri.uikit.KiriTheme
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography

// 저장한 기록 하나에 대한 일정 잡기·삭제하기 시트 (Figma 8103:74427)
@Composable
fun RecordItemOptionSheet(
    title: String,
    subtitle: String,
    onScheduleClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
    scheduleLabel: String = "일정 잡기",
    scheduleEnabled: Boolean = true,
    deleteEnabled: Boolean = true,
) {
    RecordModalSheet(onDismiss = onDismiss, testTag = "record_item_option_sheet", containerColor = KiriColor.Bg.Primary) {
        RecordItemOptionSheetContent(title, subtitle, onScheduleClick, onDeleteClick, onDismiss, scheduleLabel, scheduleEnabled, deleteEnabled)
    }
}

@Composable
internal fun RecordItemOptionSheetContent(
    title: String,
    subtitle: String,
    onScheduleClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCloseClick: () -> Unit,
    scheduleLabel: String = "일정 잡기",
    scheduleEnabled: Boolean = true,
    deleteEnabled: Boolean = true,
) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 30.dp, end = 30.dp, top = 44.dp, bottom = 44.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                // TODO: 기록 사진이 연결되면 이미지로 교체 (Figma 에서도 빈 썸네일 자리)
                Box(Modifier.size(87.dp).background(KiriColor.Bg.Tertiary, RoundedCornerShape(9.56.dp)))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, color = KiriColor.Text.Strong, style = KiriDetailTypography.Title)
                    Text(subtitle, color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small)
                }
            }
            Spacer(Modifier.height(30.dp))
            HorizontalDivider(thickness = 1.dp, color = KiriColor.Border.Default)
            Spacer(Modifier.height(20.dp))
            RecordOptionRow(
                icon = R.drawable.ic_record_check_circle,
                text = scheduleLabel,
                textColor = KiriColor.Text.Strong,
                onClick = onScheduleClick,
                testTag = "record_option_schedule",
                enabled = scheduleEnabled,
            )
            Spacer(Modifier.height(19.dp))
            HorizontalDivider(thickness = 1.dp, color = KiriColor.Border.Strong)
            Spacer(Modifier.height(19.dp))
            RecordOptionRow(
                icon = R.drawable.ic_record_delete,
                text = "삭제하기",
                textColor = KiriColor.Status.Error,
                onClick = onDeleteClick,
                testTag = "record_option_delete",
                enabled = deleteEnabled,
            )
        }
        // 닫기 표시는 위 25, 오른쪽 30 위치의 19 크기. 누르는 영역만 48 로 넓힌다
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.5.dp, end = 15.5.dp)
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onCloseClick)
                .semantics { contentDescription = "닫기" }
                .testTag("record_option_close"),
            contentAlignment = Alignment.Center,
        ) {
            RecordCloseMark(KiriColor.Bg.Elevated)
        }
    }
}

@Composable
private fun RecordOptionRow(
    icon: Int,
    text: String,
    textColor: Color,
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = textColor, style = KiriDetailTypography.Title)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            // 뒤로 아이콘을 좌우로 뒤집어 오른쪽 화살표로 쓴다
            Image(painterResource(R.drawable.ic_record_back), contentDescription = null, modifier = Modifier.scale(scaleX = -1f, scaleY = 1f))
        }
    }
}

// Figma 8103:74495: 두께 3.54, 길이 23.227 인 둥근 막대 두 개를 ±45도로 겹친 X
@Composable
internal fun RecordCloseMark(color: Color, strokeWidth: Dp = 3.54.dp, length: Dp = 23.227.dp) {
    Canvas(Modifier.size(19.dp)) {
        val stroke = strokeWidth.toPx()
        val half = (length.toPx() - stroke) / 2f / Math.sqrt(2.0).toFloat()
        val c = center
        drawLine(color, Offset(c.x - half, c.y - half), Offset(c.x + half, c.y + half), stroke, StrokeCap.Round)
        drawLine(color, Offset(c.x - half, c.y + half), Offset(c.x + half, c.y - half), stroke, StrokeCap.Round)
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true, widthDp = 390)
@Composable
private fun RecordItemOptionSheetPreview() {
    KiriTheme {
        Box(Modifier.background(KiriColor.Bg.Primary, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))) {
            RecordItemOptionSheetContent("광장시장에서 시장음식 먹어보기", "아빠가 저장했어요!", {}, {}, {})
        }
    }
}
