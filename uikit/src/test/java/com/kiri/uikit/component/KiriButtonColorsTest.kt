package com.kiri.uikit.component

import com.kiri.uikit.color.KiriComponentColor
import org.junit.Assert.assertEquals
import org.junit.Test

class KiriButtonColorsTest {

    private val accent = KiriComponentColor.Button.FilledAccent
    private val neutral = KiriComponentColor.Button.FilledNeutral

    @Test
    fun 기본_상태에서는_Default_색상을_쓴다() {
        assertEquals(
            KiriButtonColors(accent.BgDefault, accent.ContentDefault),
            kiriButtonColors(KiriButtonTone.Accent, enabled = true, pressed = false),
        )
        assertEquals(
            KiriButtonColors(neutral.BgDefault, neutral.ContentDefault),
            kiriButtonColors(KiriButtonTone.Neutral, enabled = true, pressed = false),
        )
    }

    @Test
    fun 눌린_상태에서는_Pressed_색상을_쓴다() {
        assertEquals(
            KiriButtonColors(accent.BgPressed, accent.ContentPressed),
            kiriButtonColors(KiriButtonTone.Accent, enabled = true, pressed = true),
        )
        assertEquals(
            KiriButtonColors(neutral.BgPressed, neutral.ContentPressed),
            kiriButtonColors(KiriButtonTone.Neutral, enabled = true, pressed = true),
        )
    }

    @Test
    fun 비활성_상태는_눌림보다_우선한다() {
        assertEquals(
            KiriButtonColors(accent.BgDisabled, accent.ContentDisabled),
            kiriButtonColors(KiriButtonTone.Accent, enabled = false, pressed = true),
        )
        assertEquals(
            KiriButtonColors(neutral.BgDisabled, neutral.ContentDisabled),
            kiriButtonColors(KiriButtonTone.Neutral, enabled = false, pressed = true),
        )
    }
}
