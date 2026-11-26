package com.kiri.feature.mypage

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kiri.feature.mypage.model.address.HomeAddressUiEvent
import com.kiri.feature.mypage.presentation.address.PostcodePicker
import com.kiri.uikit.KiriTheme
import java.net.URLEncoder
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PostcodePickerTest {
    @get:Rule val compose = createComposeRule()
    @Test fun webSelectionDeliversPostcodeAddressAndLegalCode() {
        var selected: HomeAddressUiEvent.AddressSelected? = null
        var selections = 0
        val address = URLEncoder.encode("서울 서초구 강남대로 143", "UTF-8")
        val page = """<html><body><script>function select(){location.href='kiri-address://selected?zip=06134&address=$address&region=1165010800';}setTimeout(select,300);setTimeout(select,900);</script></body></html>"""
        compose.setContent { KiriTheme { PostcodePicker({ selected = it; selections++ }, page) } }
        compose.waitUntil(10_000) { selections == 2 }
        assertEquals("06134", selected!!.zipCode)
        assertEquals("서울 서초구 강남대로 143", selected!!.roadAddress)
        assertEquals("1165010800", selected!!.regionCode)
    }
    @Test fun loadFailureOffersRetryAndCreatesNewWebView() {
        val page = """<html><body><script>setTimeout(function(){location.href='kiri-address://error';},300);</script></body></html>"""
        compose.setContent { KiriTheme { PostcodePicker({}, page) } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("postcode_error").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("postcode_retry").performClick()
        compose.onNodeWithTag("postcode_web_1").assertExists()
    }
}
