package com.kiri.feature.record.presentation.bucket

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
internal fun RecordBucketList(state: RecordUiState, onEvent: (RecordUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().testTag("record_bucket_list")) {
        Row(Modifier.fillMaxWidth().padding(top = 32.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SaverChip("전체", state.selectedSaverId == null, { onEvent(RecordUiEvent.SaverSelected(null)) }, "record_saver_all", highlightSelected = false)
            state.savers.forEach { saver ->
                val count = state.buckets.count { it.saverId == saver.id }
                SaverChip("${saver.nickname} $count", state.selectedSaverId == saver.id,
                    { onEvent(RecordUiEvent.SaverSelected(saver.id)) }, "record_saver_${saver.id}")
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("record_bucket_cards"), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (state.visibleBuckets.isEmpty()) item {
                Text("저장한 활동이 없어요", style = KiriDetailTypography.Title, color = KiriColor.Text.Secondary, modifier = Modifier.padding(vertical = 32.dp))
            }
            items(state.visibleBuckets, key = { it.id }) { bucket ->
                BucketCard(bucket, state.saverName(bucket.saverId)) { onEvent(RecordUiEvent.BucketClicked(bucket.id)) }
            }
        }
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

@Composable
private fun BucketCard(bucket: RecordBucketItem, saverName: String, onOpen: () -> Unit) {
    Surface(onClick = onOpen, color = KiriColor.Bg.Primary, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().testTag("record_bucket_${bucket.id}")) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (bucket.completed) BucketBadge("완료")
                val last = saverName.lastOrNull()
                val particle = if (last != null && last in '가'..'힣' && (last.code - '가'.code) % 28 != 0) "이" else "가"
                BucketBadge("$saverName$particle 저장했어요!")
            }
            Text(bucket.title, style = KiriDetailTypography.Title.copy(fontSize = 20.sp, lineHeight = 30.sp, letterSpacing = (-.8).sp), color = KiriColor.Text.Strong)
        }
    }
}

@Composable
private fun BucketBadge(text: String) {
    Surface(color = KiriColor.Bg.Tertiary, shape = RoundedCornerShape(5.dp)) {
        Text(text, style = KiriDetailTypography.Small.copy(fontWeight = FontWeight.Medium, lineHeight = 20.sp), color = KiriColor.Text.Secondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
    }
}
