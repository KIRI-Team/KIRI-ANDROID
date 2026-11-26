package com.kiri.uikit.component

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.*
import org.junit.Test

class KiriTooltipPlacementTest {
    @Test fun pointerTouchesEveryAvatarCenterAfterBodyClamping() {
        for (scale in listOf(1, 2, 3)) {
            for (center in listOf(40, 100, 190, 350)) {
                val anchor = IntRect((center - 22) * scale, 100 * scale, (center + 22) * scale, 144 * scale)
                val result = calculateKiriTooltipPlacement(anchor, IntSize(390 * scale, 844 * scale), IntSize(303 * scale, 110 * scale), 18 * scale, 72 * scale)
                assertEquals(center * scale.toFloat(), result.offset.x + result.pointerX, .01f)
                assertEquals(anchor.bottom, result.offset.y)
                assertTrue(result.below)
                assertTrue(result.offset.x >= 18 * scale)
                assertTrue(result.offset.x + 303 * scale <= 372 * scale)
            }
        }
    }

    @Test fun shortWindowPlacesBodyAboveAndBottomTipTouchesAvatar() {
        val anchor = IntRect(240, 340, 284, 384)
        val result = calculateKiriTooltipPlacement(anchor, IntSize(320, 400), IntSize(284, 140), 18, 72)
        assertFalse(result.below)
        assertEquals(anchor.top, result.offset.y + 140)
        assertEquals(262f, result.offset.x + result.pointerX, .01f)
    }

    @Test fun halfPixelCentersAreNotRoundedAway() {
        val result = calculateKiriTooltipPlacement(IntRect(18, 30, 63, 75), IntSize(360, 640), IntSize(303, 110), 18, 72)
        assertEquals(40.5f, result.offset.x + result.pointerX, .01f)
    }
}
