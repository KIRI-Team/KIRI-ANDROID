package com.kiri.feature.auth.presentation.intro

import androidx.compose.animation.core.LinearEasing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginIntroMotionTest {

    @Test
    fun 트랙은_키프레임_사이를_보간하고_범위_밖에서는_끝값을_유지한다() {
        val track = Track(Key(100f, 0f, LinearEasing), Key(200f, 10f))

        assertEquals(0f, track.valueAt(0f), 0.0001f)
        assertEquals(5f, track.valueAt(150f), 0.0001f)
        assertEquals(10f, track.valueAt(500f), 0.0001f)
    }

    @Test
    fun 구간의_easing_은_시작_키프레임의_것을_쓴다() {
        val track = Track(Key(0f, 0f, SpringSettle), Key(100f, 1f))

        // 스프링은 절반 시점에 이미 직선보다 많이 진행한다
        assertTrue(track.valueAt(50f) > 0.8f)
    }

    @Test
    fun 샘플_easing_은_처음과_끝을_샘플값으로_맞춘다() {
        assertEquals(0f, SpringOvershoot.transform(0f), 0.0001f)
        assertEquals(0.9993f, SpringOvershoot.transform(1f), 0.0001f)
        // 중간에 1을 살짝 넘었다가 돌아온다
        assertTrue(SpringOvershoot.transform(0.46f) > 1f)
    }

    @Test
    fun 처음에는_조각만_보이고_로고와_캐릭터와_버튼은_숨어_있다() = with(LoginIntroMotion) {
        pieces.forEach {
            assertEquals(1f, it.alpha.valueAt(0f), 0.0001f)
            assertEquals(0f, it.green.valueAt(0f), 0.0001f)
        }
        assertEquals(0f, wordmarkAlpha.valueAt(0f), 0.0001f)
        assertEquals(0f, characterAlpha.valueAt(0f), 0.0001f)
        assertEquals(0f, buttonsAlpha.valueAt(0f), 0.0001f)
    }

    @Test
    fun 끝나면_조각은_사라지고_로고는_72퍼센트_크기로_캐릭터는_로고_위에_자리_잡는다() = with(LoginIntroMotion) {
        pieces.forEach { assertEquals(0f, it.alpha.valueAt(END_MS), 0.0001f) }
        assertEquals(1f, wordmarkAlpha.valueAt(END_MS), 0.0001f)
        assertEquals(0.72f, wordmarkScale.valueAt(END_MS), 0.0001f)
        assertEquals(1f, characterAlpha.valueAt(END_MS), 0.0001f)
        assertEquals(0f, characterRotation.valueAt(END_MS), 0.0001f)
        assertEquals(1f, characterScale.valueAt(END_MS), 0.0001f)

        val characterBottom = CHARACTER_CENTER_Y + characterTranslateY.valueAt(END_MS) + 28f
        val wordmarkTop = WORDMARK_CENTER_Y - 99.531f * 0.72f / 2
        assertTrue("캐릭터가 로고보다 위에 있어야 한다", characterBottom < wordmarkTop)
    }

    @Test
    fun 로그인_버튼은_캐릭터가_떨어지기_시작한_뒤에_나타나기_시작해_끝나기_전에_다_보인다() = with(LoginIntroMotion) {
        assertEquals(0f, buttonsAlpha.valueAt(2900f), 0.0001f)
        assertTrue(buttonsAlpha.valueAt(3000f) > 0f)
        assertEquals(1f, buttonsAlpha.valueAt(3300f), 0.0001f)
        assertTrue(3300f < END_MS)
    }
}
