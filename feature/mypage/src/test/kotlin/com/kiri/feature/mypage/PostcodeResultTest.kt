package com.kiri.feature.mypage

import com.kiri.feature.mypage.presentation.address.parsePostcodeSelection
import java.net.URLEncoder
import org.junit.Assert.*
import org.junit.Test

class PostcodeResultTest {
    private val callback = "kiri-address://selected?zip=06134&address=" +
        URLEncoder.encode("서울 서초구 강남대로 143", "UTF-8") + "&region=1165010800"
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
