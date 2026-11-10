package com.kiri.uikit.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * 서버 사진(https)이나 기기 사진(content://, file://)을 Coil 로 불러와 그린다.
 * 메모리·디스크 캐시를 쓰고, 그리는 크기에 맞춰 줄여 디코딩한다.
 * 주소가 비어 있거나, 불러오는 중이거나, 불러오지 못하면 [placeholder] 를 대신 보여준다.
 *
 * Coil 은 앱의 OkHttpClient 가 아닌 기본 클라이언트를 쓴다. 사진 주소(presigned 등)에 로그인 토큰이 붙지 않게 하려는 것이다.
 */
@Composable
fun KiriAsyncImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable BoxScope.() -> Unit = {},
) {
    val url = model?.takeIf { it.isNotBlank() }
    // 성공하기 전까지(빈 주소·불러오는 중·실패) placeholder 를 보여준다
    var loaded by remember(url) { mutableStateOf(false) }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(true).build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onState = { state -> loaded = state is AsyncImagePainter.State.Success },
            )
        }
        if (!loaded) placeholder()
    }
}
