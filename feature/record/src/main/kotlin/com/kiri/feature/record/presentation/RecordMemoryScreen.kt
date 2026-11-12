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

// 기록 카드 한 장. 기기 기록(FamilyMemory)과 서버 리뷰(ScheduleReview)를 같은 모양으로 보여준다
data class MemoryCardUi(
    val id: String,
    val authorName: String,
    // "딸" 처럼 작성자의 가족 역할. 모르면 빈 문자열
    val role: String,
    val text: String,
    val photos: List<RecordPhoto>,
    // 내가 쓴 기록이면 수정·삭제를 보여준다 (수정·삭제 동작을 넘긴 경우에만)
    val mine: Boolean,
)

@Composable
fun RecordMemoryScreen(
    state: RecordDetailUiState,
    onBack: () -> Unit,
    onChangePhoto: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismissToast: () -> Unit,
    onReview: (() -> Unit)? = null,
) {
    val me = state.collection.currentUserId
    RecordMemoryContent(
        familyName = state.collection.familyName,
        title = state.detail.detail?.title ?: state.entry?.title.orEmpty(),
        date = state.entry?.date,
        participants = state.detail.detail?.participants.orEmpty(),
        representativePhoto = state.representativePhoto,
        memories = state.memories.map { MemoryCardUi(it.id, it.authorName, it.role, it.text, it.photos, it.authorId == me) },
        onBack = onBack,
        onChangePhoto = onChangePhoto,
        onEdit = onEdit,
        onDelete = onDelete,
        saving = state.detail.saving,
        error = state.detail.error,
        toast = state.toast,
        onDismissToast = onDismissToast,
        onReview = onReview?.takeIf { me in state.entry?.participantIds.orEmpty() && state.memories.none { it.authorId == me } },
    )
}

// Figma 7903:1926 (사진 클릭시 화면). 완료된 약속의 정보, 대표 사진, 가족의 기록 모아보기를 보여준다
// onChangePhoto·onEdit·onDelete·onReview 를 넘기지 않으면 그 동작(사진 변경, 수정하기, 삭제, 나의 추억 기록하기)을 숨긴다
@Composable
internal fun RecordMemoryContent(
    familyName: String?,
    title: String,
    date: java.time.LocalDate?,
    participants: List<com.kiri.domain.detail.DetailParticipant>,
    representativePhoto: RecordPhoto?,
    memories: List<MemoryCardUi>,
    onBack: () -> Unit,
    onChangePhoto: (() -> Unit)? = null,
    onEdit: ((String) -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
    saving: Boolean = false,
    error: String? = null,
    toast: String? = null,
    onDismissToast: () -> Unit = {},
    onReview: (() -> Unit)? = null,
    // 기록을 불러오는 중이면 빈 화면 대신 불러오는 중을, 불러오지 못했으면(onRetryMemories 가 있으면) 다시 시도를 보여준다
    memoriesLoading: Boolean = false,
    onRetryMemories: (() -> Unit)? = null,
) {
    var photoActions by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    val family = familyName?.takeIf { it.isNotBlank() } ?: "우리 가족"
    Box(Modifier.fillMaxSize().background(KiriColor.Bg.Base).testTag("record_memory_screen")) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            RecordMemoryAppBar("$family 기록", onBack)
            Column(Modifier.padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(21.dp))
                Text("약속 완료", Modifier.background(KiriColor.Bg.Elevated, RoundedCornerShape(5.dp)).padding(horizontal = 8.dp, vertical = 6.dp), color = KiriColor.Text.Primary, style = KiriDetailTypography.Small)
                Spacer(Modifier.height(11.dp))
                Text(title, style = KiriDetailTypography.Display.copy(letterSpacing = (-1.12).sp), color = KiriColor.Text.Strong)
                Spacer(Modifier.height(6.dp))
                Text(date?.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")).orEmpty(), style = KiriDetailTypography.Body, color = KiriColor.Text.Secondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("참여인원  I", style = KiriDetailTypography.Body, color = KiriColor.Text.Secondary)
                    participants.forEach { participant ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(participant.name, style = KiriDetailTypography.Body, color = KiriColor.Text.Secondary)
                            if (participant.participating) Image(painterResource(UiR.drawable.kiri_detail_check), "참여", Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(17.dp))
                Box(Modifier.fillMaxWidth().aspectRatio(354f / 185f).clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = onChangePhoto != null) { photoActions = !photoActions }.testTag("record_representative_photo")) {
                    representativePhoto?.let { RecordPhotoImage(it, Modifier.fillMaxSize()) }
                    if (photoActions && onChangePhoto != null) {
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
                Text("${family}님의 기록 모아보기", style = KiriDetailTypography.Title, color = KiriColor.Text.Strong)
                Spacer(Modifier.height(24.dp))
                when {
                    memoriesLoading -> Box(Modifier.fillMaxWidth().height(225.dp).testTag("record_memory_loading"), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KiriColor.Primary.Default)
                    }
                    onRetryMemories != null -> Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("기록을 불러오지 못했어요.", style = KiriDetailTypography.Body, color = KiriColor.Text.Secondary)
                        KiriDetailButton("다시 시도", onRetryMemories, Modifier.testTag("record_memory_retry"))
                    }
                    // Figma 9898:17487 (KIRI/Empty State)
                    memories.isEmpty() -> {
                        com.kiri.uikit.component.KiriEmptyState("아직 기록이 없어요!", Modifier.testTag("record_memory_empty"))
                        Spacer(Modifier.height(16.dp))
                    }
                    else -> memories.forEach { memory ->
                        FamilyMemoryCard(
                            memory,
                            saving,
                            onEdit?.takeIf { memory.mine }?.let { edit -> { edit(memory.id) } },
                            onDelete?.takeIf { memory.mine }?.let { { deleteId = memory.id } },
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
                if (onReview != null) {
                    KiriButton(onReview, text = "나의 추억도 기록하기", layout = KiriButtonLayout.Record)
                }
                error?.let { Text(it, color = KiriColor.Status.Error, style = KiriDetailTypography.Small, modifier = Modifier.testTag("record_mutation_error")) }
                Spacer(Modifier.height(70.dp))
                Text("연결된 가족만 볼 수 있는 기록이에요", Modifier.fillMaxWidth(), color = KiriColor.Text.Tertiary, style = KiriDetailTypography.Small, textAlign = TextAlign.Center)
                Spacer(Modifier.height(140.dp))
            }
        }
        RecordToast(toast, onDismissToast)
    }
    if (onDelete != null) deleteId?.let { id ->
        AlertDialog(onDismissRequest = { deleteId = null }, containerColor = KiriColor.Bg.Primary,
            title = { Text("기록을 삭제할까요?") }, text = { Text("삭제한 기록은 다시 복구할 수 없어요.") },
            confirmButton = { TextButton(onClick = { deleteId = null; onDelete(id) }, modifier = Modifier.testTag("confirm_delete_record")) { Text("삭제하기", color = KiriColor.Status.Error) } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("취소") } })
    }
}

// onEdit·onDelete 가 없으면 수정하기·메뉴를 숨긴다 (내 기록이 아니거나 수정 기능이 없는 경우)
@Composable
internal fun FamilyMemoryCard(memory: MemoryCardUi, saving: Boolean, onEdit: (() -> Unit)?, onDelete: (() -> Unit)?) {
    var menu by remember { mutableStateOf(false) }
    val menuOffset = with(LocalDensity.current) { 29.dp.roundToPx() }
    Box(Modifier.fillMaxWidth().background(KiriColor.Bg.Primary, RoundedCornerShape(16.dp)).testTag("family_memory_${memory.id}")) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.record_memory_avatar), null, Modifier.size(36.dp))
            Text(if (memory.role.isBlank()) memory.authorName else "${memory.authorName} I ${memory.role}", Modifier.weight(1f).padding(start = 14.dp), style = KiriDetailTypography.Body, color = KiriColor.Text.Primary)
            if (onEdit != null) {
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
    if (onEdit != null || onDelete != null) Box(Modifier.align(Alignment.TopEnd).padding(end = 8.dp).offset(y = (-4).dp)) {
        IconButton(onClick = { menu = true }, enabled = !saving, modifier = Modifier.size(36.dp).testTag("record_memory_menu_${memory.id}")) {
            Image(painterResource(R.drawable.record_memory_more), "기록 메뉴", Modifier.size(16.dp, 4.dp))
        }
        if (menu) Popup(alignment = Alignment.TopEnd, offset = IntOffset(0, menuOffset), onDismissRequest = { menu = false }, properties = PopupProperties(focusable = true)) {
            Surface(color = KiriColor.Bg.Tertiary, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, KiriColor.Border.Default)) {
                Column(Modifier.width(IntrinsicSize.Max).widthIn(min = 99.34.dp)) {
                    if (onEdit != null) MemoryMenuRow("수정하기", R.drawable.record_memory_edit_small, "menu_edit_record") { menu = false; onEdit() }
                    if (onEdit != null && onDelete != null) HorizontalDivider(color = KiriColor.Border.Default)
                    if (onDelete != null) MemoryMenuRow("삭제하기", R.drawable.record_memory_delete, "menu_delete_record") { menu = false; onDelete() }
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
    RecordPhotoSelectionContent(
        photos = state.photos,
        currentPhotoId = state.entry?.representativePhotoId,
        selectedPhotoId = state.selectedPhotoId,
        saving = state.detail.saving,
        error = state.detail.error,
        onBack = onBack, onSelect = onSelect, onSave = onSave,
    )
}

// 대표 사진 변경(사진변경). 로컬 기록과 서버 일정(리뷰 사진)이 함께 쓴다. 지금 대표 사진에는 "현재사진"을 표시하고,
// 다른 사진을 골랐을 때만 저장할 수 있다
@Composable
fun RecordPhotoSelectionContent(
    photos: List<RecordPhoto>,
    currentPhotoId: String?,
    selectedPhotoId: String?,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(KiriColor.Bg.Base).statusBarsPadding().navigationBarsPadding().testTag("record_photo_selection")) {
        RecordMemoryAppBar("사진변경", onBack)
        Spacer(Modifier.height(28.dp))
        Text("변경할 사진을 선택해주세요", Modifier.fillMaxWidth().padding(horizontal = 18.dp), style = KiriDetailTypography.Display.copy(fontSize = 24.sp, fontWeight = FontWeight.Normal, lineHeight = 36.sp, letterSpacing = (-.48).sp), color = KiriColor.Text.Strong, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("변경된 내용은 모든 가족 구성원에게 반영됩니다", Modifier.fillMaxWidth().padding(horizontal = 18.dp), style = KiriDetailTypography.Body, color = KiriColor.Text.Tertiary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.weight(1f).fillMaxWidth().testTag("record_photo_grid"), contentPadding = PaddingValues(horizontal = 36.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            items(photos, key = { it.id }) { photo ->
                val selected = selectedPhotoId == photo.id
                Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(4.dp)).selectable(selected, enabled = !saving, role = Role.RadioButton) { onSelect(photo.id) }.testTag("record_photo_${photo.id}")) {
                    RecordPhotoImage(photo, Modifier.fillMaxSize())
                    if (photo.id == currentPhotoId) {
                        Box(Modifier.fillMaxSize().background(KiriColor.Bg.Base.copy(alpha = .45f)))
                        Text("현재사진", Modifier.align(Alignment.Center).background(KiriColor.Bg.Tertiary, RoundedCornerShape(100.dp)).padding(horizontal = 14.dp, vertical = 6.dp), color = KiriColor.Text.Primary, style = KiriDetailTypography.Small)
                    } else if (selected) {
                        Box(Modifier.fillMaxSize().background(KiriColor.Primary.Default.copy(alpha = .2f)))
                        Image(painterResource(R.drawable.record_memory_selected), "선택됨", Modifier.size(32.dp).align(Alignment.Center))
                    }
                }
            }
        }
        error?.let { Text(it, Modifier.padding(horizontal = 18.dp, vertical = 8.dp), color = KiriColor.Status.Error, style = KiriDetailTypography.Small) }
        KiriButton(onSave, Modifier.padding(horizontal = 18.dp, vertical = 16.dp).testTag("save_record_photo"), text = if (saving) "저장 중…" else "저장하기",
            enabled = !saving && selectedPhotoId != null && selectedPhotoId != currentPhotoId && photos.any { it.id == selectedPhotoId })
    }
}

@Composable
private fun RecordToast(message: String?, onDismiss: () -> Unit) {
    LaunchedEffect(message) { if (message != null) { delay(3000); onDismiss() } }
    if (message != null) com.kiri.uikit.component.KiriToast(
        message,
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp).testTag("record_success_toast"),
        icon = com.kiri.uikit.component.KiriToastIcon.Check,
    )
}

@Composable
private fun RecordMemoryAppBar(title: String, onBack: () -> Unit) {
    KiriAppBar(title, onBack = onBack, leadingIconSlot = {
        IconButton(onBack, Modifier.size(44.dp)) {
            Image(painterResource(R.drawable.record_memory_back), "뒤로", Modifier.size(7.529.dp, 15.059.dp))
        }
    })
}
