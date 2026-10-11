package com.kiri.feature.mypage.presentation.suggestion.component

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.feature.mypage.R
import com.kiri.uikit.color.KiriColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TileSize = 137.dp
private val TileShape = RoundedCornerShape(8.dp)

// KIRI/Photo Upload: 누르면 사진을 고르는 137×137 칸
@Composable
fun SuggestionPhotoAddTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(TileSize)
            .clip(TileShape)
            .background(KiriColor.Bg.Secondary)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "사진 추가" }
            .testTag("suggestion_photo_add"),
        contentAlignment = Alignment.Center,
    ) {
        Image(painter = painterResource(R.drawable.ic_mypage_plus), contentDescription = null)
    }
}

// 고른 사진 미리보기. 오른쪽 위 버튼으로 뺄 수 있다
// TODO: 사진을 고른 뒤의 디자인이 나오면 미리보기와 빼기 버튼 모양을 맞춘다
@Composable
fun SuggestionPhotoTile(
    uri: String,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { TileSize.roundToPx() }
    val image by produceState<ImageBitmap?>(initialValue = null, uri, sizePx) {
        value = withContext(Dispatchers.IO) { context.loadThumbnail(Uri.parse(uri), sizePx) }
    }
    Box(
        modifier = modifier
            .size(TileSize)
            .clip(TileShape)
            .background(KiriColor.Bg.Secondary)
            .testTag("suggestion_photo"),
    ) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = "고른 사진",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        RemoveButton(
            onClick = onRemoveClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
        )
    }
}

@Composable
private fun RemoveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(KiriColor.State.Overlay)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "사진 빼기" }
            .testTag("suggestion_photo_remove"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            val stroke = 1.5.dp.toPx()
            drawLine(KiriColor.Icon.Strong, Offset.Zero, Offset(size.width, size.height), stroke, StrokeCap.Round)
            drawLine(KiriColor.Icon.Strong, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
        }
    }
}

// 큰 사진을 그대로 읽지 않도록 칸 크기에 맞게 줄여서 읽는다. 읽지 못하면 null
private fun Context.loadThumbnail(uri: Uri, targetPx: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= targetPx && bounds.outHeight / (sampleSize * 2) >= targetPx) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}.getOrNull()?.asImageBitmap()

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun SuggestionPhotoAddTilePreview() {
    SuggestionPhotoAddTile(onClick = {})
}
