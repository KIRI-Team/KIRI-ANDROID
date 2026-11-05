package com.kiri.feature.record.presentation.bucket

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.kiri.domain.model.bucket.BucketItem
import com.kiri.domain.model.bucket.BucketItemStatus
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.feature.record.model.*
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography

// 서버의 가족 버킷리스트. 칩의 개수는 서버가 준 가족 전체 기준 저장 개수이고, 칩을 고르면 그 구성원 것만 다시 불러온다
// 끝에 가까워지면 다음 페이지를 이어 붙인다
@Composable
internal fun RecordBucketList(state: RecordUiState, onEvent: (RecordUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val currentOnEvent by rememberUpdatedState(onEvent)
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, state.bucketHasNext, state.bucketItems.size) {
        if (nearEnd && state.bucketHasNext && !state.bucketLoading && !state.bucketLoadingMore && state.bucketError == null)
            currentOnEvent(RecordUiEvent.BucketListEndReached)
    }
    Column(modifier.fillMaxWidth().testTag("record_bucket_list")) {
        Row(Modifier.fillMaxWidth().padding(top = 32.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SaverChip("전체", state.selectedSaverId == null, { onEvent(RecordUiEvent.SaverSelected(null)) }, "record_saver_all", highlightSelected = false)
            state.bucketSavers.forEach { saver ->
                val id = saver.memberId.toString()
                SaverChip("${saver.nickname} ${saver.bucketCount}", state.selectedSaverId == id,
                    { onEvent(RecordUiEvent.SaverSelected(id)) }, "record_saver_$id")
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("record_bucket_cards"), state = listState, contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (state.bucketItems.isEmpty()) item(key = "bucket-empty") {
                when {
                    state.bucketLoading -> Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.testTag("record_bucket_loading"))
                    }
                    state.bucketError != null -> BucketRetry(state.bucketError, onEvent)
                    else -> Text("저장한 활동이 없어요", style = KiriDetailTypography.Title, color = KiriColor.Text.Secondary, modifier = Modifier.padding(vertical = 32.dp))
                }
            }
            items(state.bucketItems, key = { it.id }) { bucket ->
                BucketCard(bucket) { onEvent(RecordUiEvent.BucketClicked(bucket.id.toString())) }
            }
            if (state.bucketItems.isNotEmpty()) {
                if (state.bucketLoadingMore) item(key = "bucket-more") {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp).testTag("record_bucket_loading_more"))
                    }
                } else if (state.bucketError != null) item(key = "bucket-more-error") {
                    BucketRetry(state.bucketError, onEvent)
                }
            }
        }
    }
}

@Composable
private fun BucketRetry(message: String, onEvent: (RecordUiEvent) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, style = KiriDetailTypography.Small, color = KiriColor.Text.Secondary)
        TextButton({ onEvent(RecordUiEvent.BucketListRetryClicked) }, Modifier.testTag("record_bucket_retry")) { Text("다시 시도") }
    }
}

@Composable
private fun SaverChip(label: String, selected: Boolean, onClick: () -> Unit, tag: String, highlightSelected: Boolean = true) {
    val accented = selected && highlightSelected
    Surface(onClick = onClick, color = if (accented) KiriColor.Primary.Default.copy(alpha = .2f) else KiriColor.Bg.Tertiary,
        shape = RoundedCornerShape(100.dp), modifier = Modifier.height(36.dp).testTag(tag).semantics { this.selected = selected }) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(label, style = KiriDetailTypography.Small.copy(fontWeight = FontWeight.Medium, lineHeight = 20.sp), color = if (accented) KiriColor.Text.Accent else KiriColor.Text.Secondary)
        }
    }
}

// 탈퇴한 회원이 저장한 항목은 서버가 닉네임을 "탈퇴한 사용자"로 준다
@Composable
private fun BucketCard(bucket: BucketItem, onOpen: () -> Unit) {
    Surface(onClick = onOpen, color = KiriColor.Bg.Primary, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().testTag("record_bucket_${bucket.id}")) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (bucket.status == BucketItemStatus.COMPLETED) BucketBadge("완료")
                BucketBadge("${withSubjectParticle(bucket.saverNickname)} 저장했어요!")
            }
            Text(bucket.title, style = KiriDetailTypography.Title.copy(fontSize = 20.sp, lineHeight = 30.sp, letterSpacing = (-.8).sp), color = KiriColor.Text.Strong)
        }
    }
}

// 이름 뒤에 주격 조사를 붙인다. 마지막 글자에 받침이 있으면 '이', 없거나 한글이 아니면 '가' (딸이, 아빠가)
internal fun withSubjectParticle(name: String): String {
    val last = name.lastOrNull()
    val particle = if (last != null && last in '가'..'힣' && (last.code - '가'.code) % 28 != 0) "이" else "가"
    return "$name$particle"
}

@Composable
private fun BucketBadge(text: String) {
    Surface(color = KiriColor.Bg.Tertiary, shape = RoundedCornerShape(5.dp)) {
        Text(text, style = KiriDetailTypography.Small.copy(fontWeight = FontWeight.Medium, lineHeight = 20.sp), color = KiriColor.Text.Secondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
    }
}
