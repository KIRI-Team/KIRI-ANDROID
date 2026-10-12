package com.kiri.uikit.component

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

data class KiriTooltipPlacement(val offset: IntOffset, val pointerX: Float, val below: Boolean)

/** Anchor and placement use screen pixels, including the host view's system-bar offset. */
fun calculateKiriTooltipPlacement(
    anchor: IntRect,
    window: IntSize,
    popup: IntSize,
    margin: Int,
    preferredPointerX: Int,
): KiriTooltipPlacement {
    val centerX = (anchor.left + anchor.right) / 2f
    val maxLeft = (window.width - popup.width - margin).coerceAtLeast(0)
    val minLeft = margin.coerceAtMost(maxLeft)
    val left = (centerX - preferredPointerX).toInt().coerceIn(minLeft, maxLeft)
    val below = anchor.bottom + popup.height <= window.height - margin || anchor.top < popup.height + margin
    val top = if (below) anchor.bottom else anchor.top - popup.height
    return KiriTooltipPlacement(IntOffset(left, top), centerX - left, below)
}
