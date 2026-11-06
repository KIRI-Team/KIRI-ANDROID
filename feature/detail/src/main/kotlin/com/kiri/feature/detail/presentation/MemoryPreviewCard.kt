package com.kiri.feature.detail.presentation

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kiri.feature.detail.R
import com.kiri.feature.detail.model.MemoryDraft
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MemoryPhoto(uri: String, modifier: Modifier = Modifier) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                // 서버 사진(https 주소)은 한 번 내려받아 그 바이트로, 기기 사진(content 주소)은 기기에서 바로 읽는다
                val open: () -> java.io.InputStream? =
                    if (uri.startsWith("https://") || uri.startsWith("http://")) {
                        val bytes = downloadPhoto(uri)
                        ({ bytes.inputStream() })
                    } else {
                        val parsed = Uri.parse(uri)
                        ({ resolver.openInputStream(parsed) })
                    }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > 1200 || bounds.outHeight / inSampleSize > 1200) inSampleSize *= 2
                }
                open()?.use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(modifier.background(KiriColor.Bg.Tertiary, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!, "오늘의 사진", Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        else Image(painterResource(R.drawable.memory_photo_placeholder), "사진을 표시할 수 없어요", Modifier.size(width = 68.9569.dp, height = 63.2122.dp))
    }
}

// 서버가 준 사진 주소에서 사진을 내려받는다. 이미지 라이브러리를 들이기 전까지 쓰며, 따로 저장해 두지 않는다
// TODO: 목록을 다시 그릴 때마다 다시 내려받으므로 이미지 로딩 라이브러리(캐시 포함)로 바꿀 것
private fun downloadPhoto(url: String): ByteArray {
    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    connection.connectTimeout = 10_000
    connection.readTimeout = 15_000
    try {
        check(connection.responseCode in 200..299) { "사진을 내려받지 못했어요: ${connection.responseCode}" }
        return connection.inputStream.use { it.readBytes() }
    } finally {
        connection.disconnect()
    }
}

@Composable
internal fun MemoryPreviewCard(memory: MemoryDraft, author: String, onEdit: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(KiriColor.Bg.Primary, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 24.dp).testTag("local_memory_preview")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.memory_card_avatar), null, Modifier.size(36.dp))
            Text("$author I 나", Modifier.weight(1f).padding(start = 14.dp), color = KiriColor.Text.Primary, style = KiriDetailTypography.Body)
            Row(Modifier.clickable(onClick = onEdit).testTag("edit_memory"), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.memory_edit), null, Modifier.size(24.dp))
                Text("수정하기", color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small)
            }
        }
        Spacer(Modifier.height(19.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            memory.photos.forEach { MemoryPhoto(it, Modifier.width(321.dp).height(185.dp)) }
        }
        Spacer(Modifier.height(18.dp))
        Text(memory.text, color = KiriColor.Text.Primary, style = KiriDetailTypography.Body)
    }
}
