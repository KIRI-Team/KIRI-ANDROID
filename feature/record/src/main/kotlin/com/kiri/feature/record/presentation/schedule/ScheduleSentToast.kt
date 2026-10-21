package com.kiri.feature.record.presentation.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.uikit.KiriTheme
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriPretendard
import kotlinx.coroutines.delay

// 앱으로 보내기가 끝나고 돌아온 기록 탭 위에 잠깐 띄우는 알림 (Figma 앱으로보내기 8286:75889 의 KIRI/Toast 8784:9264)
@Composable
fun ScheduleSentToast(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(Unit) {
        delay(TOAST_MILLIS)
        currentOnDismiss()
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 19.dp)
            .heightIn(min = 59.dp)
            .background(KiriColor.Bg.Secondary, RoundedCornerShape(12.dp))
            .padding(start = 28.dp, end = 28.dp, top = 18.dp, bottom = 17.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("schedule_sent_toast"),
        contentAlignment = Alignment.CenterStart,
    ) {
        // KIRI/Toast/Message
        Text(
            text = "초대를 발송했습니다!",
            color = KiriColor.Text.Strong,
            style = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 18.sp, letterSpacing = (-0.72).sp),
        )
    }
}

private const val TOAST_MILLIS = 3_000L

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true, widthDp = 390)
@Composable
private fun ScheduleSentToastPreview() {
    KiriTheme { ScheduleSentToast(onDismiss = {}) }
}
