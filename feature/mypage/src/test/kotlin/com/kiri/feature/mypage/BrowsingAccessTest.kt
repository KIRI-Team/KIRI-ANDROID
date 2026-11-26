package com.kiri.feature.mypage

import com.kiri.feature.mypage.model.common.MypageMenu
import com.kiri.feature.mypage.model.common.requiresFamily
import com.kiri.feature.mypage.model.family.FamilySettingUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 둘러보기 중에는 마이페이지의 가족 설정 메뉴만 막는다
class BrowsingAccessTest {

    @Test
    fun 가족_설정_메뉴만_가족이_있어야_쓸_수_있다() {
        assertEquals(
            setOf(MypageMenu.FAMILY_SETTING, MypageMenu.HOME_ADDRESS, MypageMenu.INVITE_LINK),
            MypageMenu.entries.filter { it.requiresFamily }.toSet(),
        )
    }

    @Test
    fun 가족_정보를_불러왔는데_가족이_없을_때만_둘러보기로_본다() {
        assertTrue(FamilySettingUiState(loading = false, familyName = null).isBrowsing)
        // 불러오는 중이거나 불러오지 못했으면 막지 않는다
        assertFalse(FamilySettingUiState(loading = true, familyName = null).isBrowsing)
        assertFalse(FamilySettingUiState(loading = false, failed = true, familyName = null).isBrowsing)
        assertFalse(FamilySettingUiState(loading = false, familyName = "끼리네").isBrowsing)
    }
}
