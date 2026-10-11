package com.kiri.uikit.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.uikit.color.KiriColor

// 약관 같은 긴 세부 내용을 하단에서 올라오는 창으로 보여준다. 끌어내리기·바깥 탭·뒤로가기로 닫힌다
// 오른쪽 위 글자 버튼(예: 동의하기, 확인)을 누르면 onActionClick 을 부른다
// TODO: Figma에 세부 내용 창 디자인이 정해지면 스타일 교체 (현재는 기존 색상 토큰으로 만든 임시 스타일)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiriDetailSheet(
    title: String,
    content: String,
    actionText: String,
    onActionClick: () -> Unit,
    onDismiss: () -> Unit,
    testTag: String = "kiri_detail_sheet",
    actionTestTag: String = "kiri_detail_sheet_action",
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = KiriColor.Bg.Secondary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = KiriColor.Border.Strong) },
        modifier = Modifier.testTag(testTag),
    ) {
        // 화면 대부분을 차지하도록 높이를 채우고, 제목줄은 고정한 채 본문만 스크롤한다
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(SHEET_HEIGHT_FRACTION),
        ) {
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = TextStyle(
                        color = KiriColor.Text.Strong,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 27.sp,
                        letterSpacing = (-0.04).em,
                    ),
                )
                Text(
                    text = actionText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onActionClick)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag(actionTestTag),
                    style = TextStyle(
                        color = KiriColor.Text.Accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp,
                        letterSpacing = (-0.04).em,
                    ),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = content,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                style = TextStyle(
                    color = KiriColor.Text.Secondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.02).em,
                ),
            )
        }
    }
}

private const val SHEET_HEIGHT_FRACTION = 0.9f
