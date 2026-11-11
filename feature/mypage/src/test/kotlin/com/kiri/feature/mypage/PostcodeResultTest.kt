package com.kiri.feature.mypage

import com.kiri.feature.mypage.presentation.address.parsePostcodeSelection
import com.kiri.feature.mypage.presentation.address.postcodePage
import java.net.URLEncoder
import org.junit.Assert.*
import org.junit.Test

class PostcodeResultTest {
    private val callback = "kiri-address://selected?zip=06134&address=" +
        URLEncoder.encode("서울 서초구 강남대로 143", "UTF-8") + "&region=1165010800"
    // 100vh 는 WebView 높이가 늦게 정해지면 0 이 되어 흰 화면이 된다. 실제 화면 높이로 맞추는지 확인한다
    @Test fun postcodePageFitsPickerToTheRealViewportHeight() {
        org.junit.Assert.assertFalse(postcodePage.contains("100vh"))
        org.junit.Assert.assertTrue(postcodePage.contains("window.innerHeight+'px'"))
        org.junit.Assert.assertTrue(postcodePage.contains("addEventListener('resize',fitPicker)"))
    }

    // 카카오 우편번호 화면을 앱 색(어두운 바탕, 초록 강조)에 맞춘다
    @Test fun postcodePageUsesAppColorTheme() {
        org.junit.Assert.assertTrue(postcodePage.contains("theme:POSTCODE_THEME"))
        listOf("bgColor:'#1A1A1A'", "searchBgColor:'#252524'", "textColor:'#D9D9D8'", "emphTextColor:'#9BEA51'")
            .forEach { org.junit.Assert.assertTrue(it, postcodePage.contains(it)) }
    }

    @Test fun decodesOfficialPostcodeFieldsWithoutLosingLegalCode() {
        val result = requireNotNull(parsePostcodeSelection(callback))
        assertEquals("06134", result.zipCode)
        assertEquals("서울 서초구 강남대로 143", result.roadAddress)
        assertEquals("1165010800", result.regionCode)
    }
    @Test fun rejectsOtherOriginsDuplicateFieldsAndIncompleteResults() {
        listOf(callback.replace("kiri-address", "https"), callback.replace("selected?", "other?"),
            callback + "&zip=99999", callback + "#ignored", callback.replace("06134", "123"),
            callback.replace("1165010800", "11"), callback.replace("&region=1165010800", ""),
            callback.replace("selected?", "selected/path?"), callback.replace("zip=06134", "zip=%ZZ"))
            .forEach { assertNull(it, parsePostcodeSelection(it)) }
    }
}
