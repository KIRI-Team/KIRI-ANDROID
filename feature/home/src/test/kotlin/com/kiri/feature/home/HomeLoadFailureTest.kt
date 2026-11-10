package com.kiri.feature.home

import com.kiri.domain.home.HomeException
import com.kiri.domain.home.HomeFailure
import com.kiri.feature.home.model.HomeLoadFailure
import com.kiri.feature.home.model.HomeUiState
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 홈 오류 화면이 연결 문제·서버 오류·로그인 만료를 구분해 안내하는지 확인한다
class HomeLoadFailureTest {

    @Test
    fun 실패_이유별로_다른_안내를_고른다() {
        assertEquals(HomeLoadFailure.SERVER, HomeLoadFailure.from(HomeException(HomeFailure.SERVER)))
        assertEquals(HomeLoadFailure.SESSION, HomeLoadFailure.from(HomeException(HomeFailure.SESSION_EXPIRED)))
        assertEquals(HomeLoadFailure.NETWORK, HomeLoadFailure.from(HomeException(HomeFailure.NETWORK)))
        assertEquals(HomeLoadFailure.NETWORK, HomeLoadFailure.from(IOException()))
        assertEquals(HomeLoadFailure.UNKNOWN, HomeLoadFailure.from(HomeException(HomeFailure.UNAVAILABLE)))
        assertEquals(HomeLoadFailure.UNKNOWN, HomeLoadFailure.from(IllegalStateException()))
    }

    @Test
    fun 실패_이유가_있으면_오류_상태이고_안내_문구가_서로_다르다() {
        assertTrue(HomeUiState(loadFailure = HomeLoadFailure.SERVER).loadError)
        assertFalse(HomeUiState().loadError)
        assertEquals(HomeLoadFailure.entries.size, HomeLoadFailure.entries.map { it.message }.toSet().size)
    }
}
