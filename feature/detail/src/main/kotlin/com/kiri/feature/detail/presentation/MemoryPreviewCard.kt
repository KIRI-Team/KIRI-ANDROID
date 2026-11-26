package com.kiri.feature.detail.presentation

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kiri.feature.detail.R
import com.kiri.feature.detail.model.MemoryDraft
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriAsyncImage
import com.kiri.uikit.component.KiriDetailTypography

// 서버 사진(https)과 기기 사진(content://, file://)을 Coil 로 그린다 (캐시와 크기 맞춤 디코딩은 Coil 이 한다).
// 불러오는 동안과 불러오지 못했을 때는 기본 사진 아이콘을 보여준다
@Composable
fun MemoryPhoto(uri: String, modifier: Modifier = Modifier, contentDescription: String = "오늘의 사진") {
    val shape = RoundedCornerShape(8.dp)
    KiriAsyncImage(uri, contentDescription, modifier.background(KiriColor.Bg.Tertiary, shape).clip(shape)) {
        Image(painterResource(R.drawable.memory_photo_placeholder), "사진을 표시할 수 없어요", Modifier.size(width = 68.9569.dp, height = 63.2122.dp))
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
