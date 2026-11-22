package com.kiri.uikit.component

import android.graphics.Bitmap
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
import coil3.BitmapImage
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade

/**
 * 서버 사진(https)이나 기기 사진(content://, file://)을 Coil 로 불러와 그린다.
 * 메모리·디스크 캐시를 쓰고(서버 사진은 서명 쿼리를 뺀 주소가 캐시 키다, [imageCacheKey]), 그리는 크기에 맞춰 줄여 디코딩한다.
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
    // 불러온 사진의 픽셀이 필요할 때(예: 밝기에 따라 글자색 고르기) 넘긴다.
    // 넘기면 픽셀을 읽을 수 있도록 하드웨어 비트맵을 쓰지 않고 디코딩한다
    onBitmap: ((Bitmap) -> Unit)? = null,
    // 불러온 이미지에 입힐 색 필터 (예: 흑백으로 흐리게)
    colorFilter: androidx.compose.ui.graphics.ColorFilter? = null,
    placeholder: @Composable BoxScope.() -> Unit = {},
) {
    val url = model?.takeIf { it.isNotBlank() }
    // 성공하기 전까지(빈 주소·불러오는 중·실패) placeholder 를 보여준다
    var loaded by remember(url) { mutableStateOf(false) }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(true)
                    .apply {
                        val cacheKey = imageCacheKey(url)
                        memoryCacheKey(cacheKey)
                        diskCacheKey(cacheKey)
                        if (onBitmap != null) allowHardware(false)
                    }
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                colorFilter = colorFilter,
                onState = { state ->
                    loaded = state is AsyncImagePainter.State.Success
                    if (state is AsyncImagePainter.State.Success) {
                        (state.result.image as? BitmapImage)?.bitmap?.let { onBitmap?.invoke(it) }
                    }
                },
            )
        }
        if (!loaded) placeholder()
    }
}

/**
 * 사진의 캐시 키. 서버 사진(https)은 서명 쿼리스트링이 요청마다 바뀌어도 같은 사진이면 캐시를 다시 쓰도록
 * 쿼리와 프래그먼트를 뺀 주소를 쓴다. 요청은 서명이 붙은 원래 주소로 보낸다.
 * 기기 사진(content://, file://)은 쿼리가 사진을 가리키는 데 쓰일 수 있어 그대로 둔다
 */
internal fun imageCacheKey(url: String): String {
    val scheme = url.substringBefore("://", "").lowercase()
    if (scheme != "http" && scheme != "https") return url
    return url.substringBefore('#').substringBefore('?')
}
