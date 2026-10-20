package com.kiri.feature.record.presentation.sheet

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kiri.feature.record.R
import com.kiri.uikit.KiriTheme
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.color.KiriComponentColor
import com.kiri.uikit.component.KiriDetailTypography
import kotlin.math.roundToInt

data class RecordMember(val id: String, val name: String)

// 일정에 함께할 구성원을 여러 명 고르는 시트 (Figma 8347:5808 / 8347:5990 / 8347:6302)
@Composable
fun RecordMemberSelectSheet(
    members: List<RecordMember>,
    selectedIds: Set<String>,
    onMemberToggle: (RecordMember) -> Unit,
    onConfirmClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    RecordModalSheet(onDismiss = onDismiss, testTag = "record_member_select_sheet") {
        RecordMemberSelectSheetContent(members, selectedIds, onMemberToggle, onConfirmClick)
    }
}

@Composable
internal fun RecordMemberSelectSheetContent(
    members: List<RecordMember>,
    selectedIds: Set<String>,
    onMemberToggle: (RecordMember) -> Unit,
    onConfirmClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().height(SheetHeight)) {
        Spacer(Modifier.height(31.dp))
        RecordSheetTitle("구성원선택")
        Spacer(Modifier.height(41.dp))
        RecordMemberGrid(members, selectedIds, onMemberToggle, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.weight(1f))
        RecordSheetBottomAction("확인", onConfirmClick)
    }
}

// 4명 이하면 카드 높이를 151 로 고정하고(8347:6439), 그보다 많으면 347 높이를 줄 수만큼 나눠 가진다(8347:5812)
@Composable
private fun RecordMemberGrid(
    members: List<RecordMember>,
    selectedIds: Set<String>,
    onMemberToggle: (RecordMember) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fixedRows = members.size <= FIXED_ROW_MAX_MEMBERS
    Column(
        modifier = modifier.width(314.dp).then(if (fixedRows) Modifier else Modifier.height(347.dp)),
        verticalArrangement = Arrangement.spacedBy(GridGap),
    ) {
        members.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().then(if (fixedRows) Modifier.height(151.dp) else Modifier.weight(1f)),
                horizontalArrangement = Arrangement.spacedBy(GridGap),
            ) {
                row.forEach { member ->
                    RecordMemberCard(member, selected = member.id in selectedIds, onClick = { onMemberToggle(member) })
                }
            }
        }
    }
}

// KIRI/Selection Card, Type=Family (8413:5630 / 8413:5645)
@Composable
private fun RecordMemberCard(member: RecordMember, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .fillMaxHeight()
            .drawBehind {
                val radius = CornerRadius(8.dp.toPx())
                // 선택 배경은 반투명이라 시트 색을 먼저 깔아 불투명하게 만든다. 그래야 아래 퍼즐 조각의 더하기 합성이 Figma 와 같게 나온다
                if (selected) drawRoundRect(KiriColor.Bg.Secondary, cornerRadius = radius)
                drawRoundRect(if (selected) KiriComponentColor.Card.BgSelected else KiriColor.Bg.Primary, cornerRadius = radius)
            }
            // 선택 상태는 배경색으로 보여주므로 눌림 물결은 끈다
            .toggleable(value = selected, interactionSource = null, indication = null, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(start = 36.dp, end = 36.dp, top = 16.dp, bottom = 8.dp)
            .testTag("record_member_${member.id}"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RecordMemberPiece(selected)
            Text(member.name, color = if (selected) KiriColor.Text.Accent else KiriColor.Text.Primary, style = KiriDetailTypography.Body)
        }
    }
}

// Figma 는 퍼즐 모양 휘도 마스크에 색을 채우고 plus-lighter 로 카드 위에 더한다.
// 마스크를 알파 이미지로 렌더링해 두고 같은 색으로 물들인 뒤 BlendMode.Plus 로 그린다
@Composable
private fun RecordMemberPiece(selected: Boolean) {
    val mask: ImageBitmap = ImageBitmap.imageResource(
        if (selected) R.drawable.img_record_member_piece_selected else R.drawable.img_record_member_piece_unselected,
    )
    val fill = if (selected) KiriColor.Bg.Green else KiriColor.Bg.Primary
    Canvas(Modifier.size(40.dp)) {
        drawImage(
            image = mask,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            colorFilter = ColorFilter.tint(fill),
            blendMode = BlendMode.Plus,
        )
    }
}

private const val FIXED_ROW_MAX_MEMBERS = 4
private val GridGap = 14.dp
private val SheetHeight = 585.dp

@Preview(backgroundColor = 0xFF2B2B2B, showBackground = true, widthDp = 390)
@Composable
private fun RecordMemberSelectSheetSixPreview() {
    var selected by remember { mutableStateOf(setOf("1", "4")) }
    KiriTheme {
        RecordMemberSelectSheetContent(
            members = (1..6).map { RecordMember(it.toString(), "마마") },
            selectedIds = selected,
            onMemberToggle = { selected = if (it.id in selected) selected - it.id else selected + it.id },
            onConfirmClick = {},
        )
    }
}

@Preview(backgroundColor = 0xFF2B2B2B, showBackground = true, widthDp = 390)
@Composable
private fun RecordMemberSelectSheetFourPreview() {
    KiriTheme {
        RecordMemberSelectSheetContent(
            members = (1..4).map { RecordMember(it.toString(), "마마") },
            selectedIds = setOf("4"),
            onMemberToggle = {},
            onConfirmClick = {},
        )
    }
}
