package com.kiri.feature.record.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.domain.record.*
import com.kiri.feature.record.R
import com.kiri.feature.record.RecordDetailUiState
import com.kiri.feature.detail.presentation.MemoryPhoto
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.*
import com.kiri.uikit.R as UiR
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/** Demo photo IDs are resolved at the presentation boundary; uploaded URIs stay dynamic. */
@Composable
fun RecordPhotoImage(photo: RecordPhoto, modifier: Modifier = Modifier) {
    val drawable = when (photo.source) {
        "demo:group" -> R.drawable.record_memory_group
        "demo:pixel" -> R.drawable.record_memory_pixel
        else -> null
    }
    if (drawable != null) Image(painterResource(drawable), "가족 활동 사진", modifier, contentScale = ContentScale.Crop)
    else MemoryPhoto(photo.source, modifier)
}

@Composable
fun RecordMemoryScreen(
    state: RecordDetailUiState,
    onBack: () -> Unit,
    onChangePhoto: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismissToast: () -> Unit,
) {
    var photoActions by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    val title = state.detail.detail?.title ?: state.entry?.title.orEmpty()
    Box(Modifier.fillMaxSize().background(KiriColor.Bg.Base).testTag("record_memory_screen")) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            RecordMemoryAppBar("${state.collection.familyName.ifBlank { "우리 가족" }} 기록", onBack)
            Column(Modifier.padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(21.dp))
                Text("약속 완료", Modifier.background(KiriColor.Bg.Elevated, RoundedCornerShape(5.dp)).padding(horizontal = 8.dp, vertical = 6.dp), color = KiriColor.Text.Primary, style = KiriDetailTypography.Small)
                Spacer(Modifier.height(11.dp))
                Text(title, style = KiriDetailTypography.Display.copy(letterSpacing = (-1.12).sp), color = KiriColor.Text.Strong)
                Spacer(Modifier.height(6.dp))
                Text(state.entry?.date?.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")).orEmpty(), style = KiriDetailTypography.Body, color = KiriColor.Text.Secondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("참여인원  I", style = KiriDetailTypography.Body, color = KiriColor.Text.Secondary)
                    state.detail.detail?.participants.orEmpty().forEach { participant ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(participant.name, style = KiriDetailTypography.Body, color = KiriColor.Text.Secondary)
                            if (participant.participating) Image(painterResource(UiR.drawable.kiri_detail_check), "참여", Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(17.dp))
                Box(Modifier.fillMaxWidth().aspectRatio(354f / 185f).clip(RoundedCornerShape(4.dp)).clickable { photoActions = !photoActions }.testTag("record_representative_photo")) {
                    state.representativePhoto?.let { RecordPhotoImage(it, Modifier.fillMaxSize()) }
                    if (photoActions) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .57f)))
                        Surface(onClick = { photoActions = false; onChangePhoto() }, color = KiriColor.Bg.Tertiary, border = BorderStroke(1.dp, KiriColor.Border.Default), shape = RoundedCornerShape(100.dp), modifier = Modifier.align(Alignment.TopStart).padding(10.dp).testTag("change_record_photo")) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Image(painterResource(R.drawable.record_memory_edit_small), null, Modifier.size(18.dp))
                                Text("사진 변경", style = KiriDetailTypography.Small, color = KiriColor.Text.Primary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(30.dp))
                Text("${state.collection.familyName.ifBlank { "우리 가족" }}님의 기록 모아보기", style = KiriDetailTypography.Title, color = KiriColor.Text.Strong)
                Spacer(Modifier.height(24.dp))
                if (state.memories.isEmpty()) Text("아직 기록이 없어요", Modifier.fillMaxWidth().padding(vertical = 48.dp), style = KiriDetailTypography.Body, color = KiriColor.Text.Tertiary, textAlign = TextAlign.Center)
                state.memories.forEach { memory ->
                    FamilyMemoryCard(memory, memory.authorId == state.collection.currentUserId, state.detail.saving,
                        { onEdit(memory.id) }, { deleteId = memory.id })
                    Spacer(Modifier.height(16.dp))
                }
                state.detail.error?.let { Text(it, color = KiriColor.Status.Error, style = KiriDetailTypography.Small, modifier = Modifier.testTag("record_mutation_error")) }
                Spacer(Modifier.height(70.dp))
                Text("연결된 가족만 볼 수 있는 기록이에요", Modifier.fillMaxWidth(), color = KiriColor.Text.Tertiary, style = KiriDetailTypography.Small, textAlign = TextAlign.Center)
                Spacer(Modifier.height(140.dp))
            }
        }
        RecordToast(state.toast, onDismissToast)
    }
    deleteId?.let { id ->
        AlertDialog(onDismissRequest = { deleteId = null }, containerColor = KiriColor.Bg.Primary,
            title = { Text("기록을 삭제할까요?") }, text = { Text("삭제한 기록은 다시 복구할 수 없어요.") },
            confirmButton = { TextButton(onClick = { deleteId = null; onDelete(id) }, modifier = Modifier.testTag("confirm_delete_record")) { Text("삭제하기", color = KiriColor.Status.Error) } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("취소") } })
    }
}

@Composable
private fun FamilyMemoryCard(memory: FamilyMemory, editable: Boolean, saving: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val menuOffset = with(LocalDensity.current) { 29.dp.roundToPx() }
    Box(Modifier.fillMaxWidth().background(KiriColor.Bg.Primary, RoundedCornerShape(16.dp)).testTag("family_memory_${memory.id}")) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.record_memory_avatar), null, Modifier.size(36.dp))
            Text("${memory.authorName} I ${memory.role}", Modifier.weight(1f).padding(start = 14.dp), style = KiriDetailTypography.Body, color = KiriColor.Text.Primary)
            if (editable) {
                Row(Modifier.clickable(enabled = !saving, onClick = onEdit).testTag("edit_record_${memory.id}"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Image(painterResource(R.drawable.record_memory_edit), null, Modifier.size(24.dp))
                    Text("수정하기", style = KiriDetailTypography.Small, color = KiriColor.Text.Secondary)
                }
            }
        }
        Spacer(Modifier.height(19.dp))
        if (memory.photos.isEmpty()) Box(Modifier.fillMaxWidth().height(185.dp).background(KiriColor.Bg.Tertiary, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.record_memory_placeholder), "기록 사진 없음", Modifier.size(68.9569.dp, 63.2122.dp))
        } else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            memory.photos.forEach { RecordPhotoImage(it, Modifier.width(321.dp).height(185.dp).clip(RoundedCornerShape(8.dp))) }
        }
        Spacer(Modifier.height(18.dp))
        Text(memory.text, style = KiriDetailTypography.Body.copy(lineHeight = 25.6.sp), color = KiriColor.Text.Primary)
    }
    if (editable) Box(Modifier.align(Alignment.TopEnd).padding(end = 8.dp).offset(y = (-4).dp)) {
        IconButton(onClick = { menu = true }, enabled = !saving, modifier = Modifier.size(36.dp).testTag("record_memory_menu_${memory.id}")) {
            Image(painterResource(R.drawable.record_memory_more), "기록 메뉴", Modifier.size(16.dp, 4.dp))
        }
        if (menu) Popup(alignment = Alignment.TopEnd, offset = IntOffset(0, menuOffset), onDismissRequest = { menu = false }, properties = PopupProperties(focusable = true)) {
            Surface(color = KiriColor.Bg.Tertiary, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, KiriColor.Border.Default)) {
                Column(Modifier.width(IntrinsicSize.Max).widthIn(min = 99.34.dp)) {
                    MemoryMenuRow("수정하기", R.drawable.record_memory_edit_small, "menu_edit_record") { menu = false; onEdit() }
                    HorizontalDivider(color = KiriColor.Border.Default)
                    MemoryMenuRow("삭제하기", R.drawable.record_memory_delete, "menu_delete_record") { menu = false; onDelete() }
                }
            }
        }
    }
    }
}

@Composable
private fun MemoryMenuRow(label: String, icon: Int, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 42.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Image(painterResource(icon), null, Modifier.size(18.dp))
        Text(label, style = KiriDetailTypography.Small.copy(lineHeight = 18.sp), color = KiriColor.Text.Secondary, maxLines = 1, softWrap = false)
    }
}

@Composable
fun RecordPhotoSelectionScreen(state: RecordDetailUiState, onBack: () -> Unit, onSelect: (String) -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxSize().background(KiriColor.Bg.Base).statusBarsPadding().navigationBarsPadding().testTag("record_photo_selection")) {
        RecordMemoryAppBar("사진변경", onBack)
        Spacer(Modifier.height(28.dp))
        Text("변경할 사진을 선택해주세요", Modifier.fillMaxWidth().padding(horizontal = 18.dp), style = KiriDetailTypography.Display.copy(fontSize = 24.sp, fontWeight = FontWeight.Normal, lineHeight = 36.sp, letterSpacing = (-.48).sp), color = KiriColor.Text.Strong, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("변경된 내용은 모든 가족 구성원에게 반영됩니다", Modifier.fillMaxWidth().padding(horizontal = 18.dp), style = KiriDetailTypography.Body, color = KiriColor.Text.Tertiary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.weight(1f).fillMaxWidth().testTag("record_photo_grid"), contentPadding = PaddingValues(horizontal = 36.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            items(state.photos, key = { it.id }) { photo ->
                val selected = state.selectedPhotoId == photo.id
                Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(4.dp)).selectable(selected, enabled = !state.detail.saving, role = Role.RadioButton) { onSelect(photo.id) }.testTag("record_photo_${photo.id}")) {
                    RecordPhotoImage(photo, Modifier.fillMaxSize())
                    if (photo.id == state.entry?.representativePhotoId) {
                        Box(Modifier.fillMaxSize().background(KiriColor.Bg.Base.copy(alpha = .45f)))
                        Text("현재사진", Modifier.align(Alignment.Center).background(KiriColor.Bg.Tertiary, RoundedCornerShape(100.dp)).padding(horizontal = 14.dp, vertical = 6.dp), color = KiriColor.Text.Primary, style = KiriDetailTypography.Small)
                    } else if (selected) {
                        Box(Modifier.fillMaxSize().background(KiriColor.Primary.Default.copy(alpha = .2f)))
                        Image(painterResource(R.drawable.record_memory_selected), "선택됨", Modifier.size(32.dp).align(Alignment.Center))
                    }
                }
            }
        }
        state.detail.error?.let { Text(it, Modifier.padding(horizontal = 18.dp, vertical = 8.dp), color = KiriColor.Status.Error, style = KiriDetailTypography.Small) }
        KiriButton(onSave, Modifier.padding(horizontal = 18.dp, vertical = 16.dp).testTag("save_record_photo"), text = if (state.detail.saving) "저장 중…" else "저장하기",
            enabled = !state.detail.saving && state.selectedPhotoId != null && state.selectedPhotoId != state.entry?.representativePhotoId && state.photos.any { it.id == state.selectedPhotoId })
    }
}

@Composable
private fun RecordToast(message: String?, onDismiss: () -> Unit) {
    LaunchedEffect(message) { if (message != null) { delay(3000); onDismiss() } }
    if (message != null) Surface(color = KiriColor.Bg.Secondary, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp).testTag("record_success_toast").semantics { liveRegion = LiveRegionMode.Polite }) {
        Row(Modifier.padding(horizontal = 28.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(R.drawable.record_memory_success), null, Modifier.size(18.dp))
            Text(message, style = KiriDetailTypography.Body.copy(fontWeight = FontWeight.Medium), color = KiriColor.Text.Strong)
        }
    }
}

@Composable
private fun RecordMemoryAppBar(title: String, onBack: () -> Unit) {
    KiriAppBar(title, onBack = onBack, leadingIconSlot = {
        IconButton(onBack, Modifier.size(44.dp)) {
            Image(painterResource(R.drawable.record_memory_back), "뒤로", Modifier.size(7.529.dp, 15.059.dp))
        }
    })
}
