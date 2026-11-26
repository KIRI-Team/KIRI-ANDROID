package com.kiri.uikit.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kiri.uikit.KiriTypography
import com.kiri.uikit.R
import com.kiri.uikit.color.KiriColor

/**
 * Figma 8450:18276 (KIRI/Empty State, Type=Record). 점선 테두리 박스 안에 문구와 흐린 끼리 캐릭터를 둔다.
 * 너비는 부모를 채우고 높이는 225dp 다.
 */
@Composable
fun KiriEmptyState(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(225.dp)
            .drawBehind {
                val stroke = 1.dp.toPx()
                val radius = 8.dp.toPx()
                drawRoundRect(
                    color = KiriColor.Border.Default,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(radius - stroke / 2),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(text, color = KiriColor.Text.Disabled, style = KiriTypography.TitleMedium)
            Box(
                Modifier.size(80.dp).alpha(0.2f).background(KiriColor.Bg.Secondary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.kiri_empty_state_character), contentDescription = null)
            }
        }
    }
}
