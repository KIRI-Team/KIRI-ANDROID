package com.kiri.feature.mypage.presentation.address

import android.annotation.SuppressLint
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import com.kiri.feature.mypage.model.address.HomeAddressUiEvent
import com.kiri.uikit.color.KiriColor
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun PostcodePicker(
    onSelected: (HomeAddressUiEvent.AddressSelected) -> Unit,
    page: String = postcodePage,
) {
    val selected by rememberUpdatedState(onSelected)
    var error by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        delay(15_000)
        if (!ready) error = "주소 검색 연결이 지연되고 있어요. 다시 시도해주세요."
    }
    Column(Modifier.fillMaxSize().testTag("postcode_picker")) {
        if (error != null) {
            Text(error!!, Modifier.padding(16.dp).testTag("postcode_error"), color = KiriColor.Text.Primary)
            TextButton({ error = null; ready = false; attempt++ },
                Modifier.testTag("postcode_retry")) { Text("다시 시도", color = KiriColor.Text.Accent) }
        } else if (!ready) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("postcode_loading"))
        key(attempt) {
            AndroidView(factory = { context -> WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                val fail = { error = "주소 검색을 불러오지 못했어요. 인터넷 연결을 확인하고 다시 시도해주세요." }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val uri = request.url
                        if (uri.scheme == "kiri-address") {
                            if (!request.isForMainFrame) return true
                            when (uri.host) {
                                "ready" -> ready = true
                                "error" -> fail()
                                "selected" -> {
                                    val result = parsePostcodeSelection(uri.toString())
                                    if (result == null) error = "선택한 주소 정보를 확인하지 못했어요. 다른 검색 결과를 선택해주세요."
                                    else { ready = true; selected(result) }
                                }
                            }
                            return true
                        }
                        // 검색 서비스 내부 프레임은 허용하되 앱 밖으로 나가는 최상위 이동은 막는다.
                        return request.isForMainFrame && !(uri.scheme == "https" &&
                            uri.host in setOf("postcode.map.kakao.com", "postcode.map.daum.net"))
                    }
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, e: WebResourceError) {
                        if (request.isForMainFrame || request.url.path?.endsWith("postcode.v2.js") == true) fail()
                    }
                    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                        if (request.isForMainFrame || request.url.path?.endsWith("postcode.v2.js") == true) fail()
                    }
                }
                loadDataWithBaseURL("https://postcode.map.kakao.com/", page, "text/html", "UTF-8", null)
            } }, modifier = Modifier.weight(1f).testTag("postcode_web_$attempt"), onRelease = { it.stopLoading(); it.destroy() })
        }
    }
}
