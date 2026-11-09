package com.kiri.feature.mypage.presentation.address

import android.annotation.SuppressLint
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import com.kiri.feature.mypage.model.address.HomeAddressUiEvent

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun PostcodePicker(onSelected: (HomeAddressUiEvent.AddressSelected) -> Unit) {
    val selected by rememberUpdatedState(onSelected)
    var error by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth().fillMaxHeight()) {
        if (error) {
            Text("주소 검색을 불러오지 못했어요. 인터넷 연결을 확인해주세요.", Modifier.padding(16.dp), color = com.kiri.uikit.color.KiriColor.Text.Primary)
            TextButton({ error = false; attempt++ }) { Text("다시 시도", color = com.kiri.uikit.color.KiriColor.Text.Accent) }
        }
        key(attempt) {
            AndroidView(factory = { context -> WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val uri = request.url
                        if (request.isForMainFrame && uri.scheme == "kiri-address" && uri.host == "selected") {
                            val zip = uri.getQueryParameter("zip").orEmpty()
                            val address = uri.getQueryParameter("address").orEmpty()
                            val region = uri.getQueryParameter("region").orEmpty()
                            if (zip.matches(Regex("[0-9]{5}")) && region.matches(Regex("[0-9]{10}")) && address.length in 1..300)
                                selected(HomeAddressUiEvent.AddressSelected(zip, address, region))
                            return true
                        }
                        return request.isForMainFrame
                    }
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, e: WebResourceError) {
                        if (request.isForMainFrame || request.url.path?.endsWith("postcode.v2.js") == true) error = true
                    }
                }
                loadDataWithBaseURL("https://postcode.map.kakao.com/", """
                    <!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
                    <body style="margin:0"><div id="picker" style="width:100%;height:100vh"></div>
                    <script src="https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>
                    <script>new kakao.Postcode({oncomplete:function(d){window.location.href='kiri-address://selected?zip='+encodeURIComponent(d.zonecode)+'&address='+encodeURIComponent(d.roadAddress||d.jibunAddress)+'&region='+encodeURIComponent(d.bcode);},width:'100%',height:'100%'}).embed(document.getElementById('picker'));</script></body></html>
                """.trimIndent(), "text/html", "UTF-8", null)
            } }, modifier = Modifier.weight(1f), onRelease = { it.stopLoading(); it.destroy() })
        }
    }
}
