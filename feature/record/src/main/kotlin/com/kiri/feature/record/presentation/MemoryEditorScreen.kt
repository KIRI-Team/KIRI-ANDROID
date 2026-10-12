package com.kiri.feature.record.presentation

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.feature.detail.R as DetailR
import com.kiri.feature.detail.model.MemoryDraft
import com.kiri.feature.detail.presentation.MemoryPhoto
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.*

/** UI-only activity review flow. No record repository or upload is performed. */
@Composable
fun MemoryEditorScreen(
    draft: MemoryDraft,
    author: String,
    onChange: (MemoryDraft) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    val resolver = LocalContext.current.contentResolver
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri -> runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        if (uris.isNotEmpty()) onChange(draft.copy(photos = (draft.photos + uris.map { it.toString() }).distinct()))
    }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(KiriColor.Bg.Base).imePadding().testTag("memory_editor")) {
        Row(Modifier.fillMaxWidth().height(62.dp), verticalAlignment = Alignment.CenterVertically) {
            KiriLeadingIconSlot(onBack)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSubmit, enabled = draft.canSubmit, modifier = Modifier.padding(end = 12.dp).testTag("submit_memory")) {
                Text("등록하기", color = if (draft.canSubmit) KiriColor.Text.Strong else KiriColor.Text.Disabled, style = KiriDetailTypography.Title)
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(painterResource(DetailR.drawable.memory_author), null, Modifier.size(width = 32.1994.dp, height = 29.5167.dp))
                Text("$author I 나", style = KiriDetailTypography.Body, color = KiriColor.Text.Primary)
            }
            Spacer(Modifier.height(36.dp))
            EditorTitle("끼리 추천은 어떠셨나요?")
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                (1..5).forEach { score ->
                    Image(painterResource(if (draft.rating >= score) DetailR.drawable.memory_star_selected else DetailR.drawable.memory_star_unselected),
                        "$score 점", Modifier.size(32.dp).selectable(draft.rating == score, role = Role.RadioButton) { onChange(draft.copy(rating = score)) }.testTag("memory_rating_$score"))
                }
            }
            Spacer(Modifier.height(44.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                EditorTitle("오늘의 사진")
                Text("(최소 1장)", color = KiriColor.Text.Tertiary, style = KiriDetailTypography.Small.copy(fontSize = 12.sp))
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                draft.photos.forEachIndexed { index, uri ->
                    Box(Modifier.size(137.dp)) {
                        MemoryPhoto(uri, Modifier.fillMaxSize())
                        TextButton(onClick = { onChange(draft.copy(photos = draft.photos - uri)) }, modifier = Modifier.align(Alignment.TopEnd).testTag("remove_memory_photo_$index")) {
                            Text("삭제", color = KiriColor.Text.Strong, style = KiriDetailTypography.Small)
                        }
                    }
                }
                Box(Modifier.size(137.dp).background(KiriColor.Bg.Secondary, RoundedCornerShape(8.dp)).clickable { photoPicker.launch(arrayOf("image/*")) }.testTag("add_memory_photo"), contentAlignment = Alignment.Center) {
                    Image(painterResource(DetailR.drawable.memory_photo_add), "사진 추가", Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(45.dp))
            EditorTitle("오늘의 하루를 돌아봐요!")
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().height(149.dp).background(KiriColor.Bg.Secondary, RoundedCornerShape(8.dp)).padding(horizontal = 23.dp, vertical = 24.dp)) {
                BasicTextField(draft.text, { onChange(draft.copy(text = it.take(500))) }, Modifier.fillMaxWidth().weight(1f).testTag("memory_text"),
                    textStyle = KiriDetailTypography.Body.copy(color = KiriColor.Text.Primary, lineHeight = 24.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(KiriColor.Text.Accent),
                    decorationBox = { field -> Box { if (draft.text.isEmpty()) Text("끼리네 가족한테 공유하고 싶은 오늘의 감정을\n알려주세요", color = KiriColor.Text.Tertiary, style = KiriDetailTypography.Body.copy(lineHeight = 24.sp)); field() } })
                Text("${draft.text.length}/500", Modifier.align(Alignment.End), color = KiriColor.Text.Tertiary, style = KiriDetailTypography.Small)
            }
            Spacer(Modifier.height(47.dp))
            EditorTitle("이 활동을 다시 참여하고 싶으신가요?")
            Spacer(Modifier.height(4.dp))
            Text("추후 AI 미션 추천에 반영됩니다", color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(true to "예", false to "아니오").forEach { (value, label) ->
                    val selected = draft.repeatActivity == value
                    Box(Modifier.weight(1f).height(54.dp).background(if (selected) KiriColor.Primary.Default else KiriColor.Bg.Secondary, RoundedCornerShape(8.dp))
                        .selectable(selected, role = Role.RadioButton) { onChange(draft.copy(repeatActivity = value)) }.testTag("memory_repeat_$value"), contentAlignment = Alignment.Center) {
                        Text(label, color = if (selected) KiriColor.Text.OnGreen else KiriColor.Text.Disabled, style = KiriDetailTypography.Body)
                    }
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun EditorTitle(text: String) { Text(text, style = KiriDetailTypography.Title.copy(lineHeight = 22.sp), color = KiriColor.Text.Strong) }
