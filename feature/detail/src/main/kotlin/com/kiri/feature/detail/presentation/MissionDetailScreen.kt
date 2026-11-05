package com.kiri.feature.detail.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.domain.detail.MissionDetail
import com.kiri.domain.mission.MissionReview
import com.kiri.domain.time.kiriToday
import com.kiri.feature.detail.DetailViewModel
import com.kiri.feature.detail.R as DetailR
import com.kiri.feature.detail.model.*
import com.kiri.uikit.R as UiR
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.*

/** Calendar occurrences share mission content but own their header and available actions.
 * neutralBadge: 배지를 회색으로 그린다 (약속 예정).
 * collapseParticipants: 참여 상태 줄 대신 "인원확인 I 날짜"를 보여주고, 누르면 참여자 말풍선을 띄운다 (Figma 9898:20930). */
data class CalendarDetailHeader(
    val title: String,
    val badge: String,
    val showCustom: Boolean,
    val neutralBadge: Boolean = false,
    val collapseParticipants: Boolean = false,
)

@Composable
fun MissionDetailRoute(
    onBack: () -> Unit,
    onCustom: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
    onRecord: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentBack by rememberUpdatedState(onBack)
    val currentCustom by rememberUpdatedState(onCustom)
    val currentRecord by rememberUpdatedState(onRecord)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    DetailUiEffect.NavigateBack -> currentBack()
                    DetailUiEffect.OpenCustom -> currentCustom()
                    DetailUiEffect.OpenRecord -> currentRecord()
                }
            }
        }
    }
    MissionDetailScreen(state, viewModel::onEvent)
}

@Composable
fun MissionDetailScreen(
    state: DetailUiState,
    onEvent: (DetailUiEvent) -> Unit,
    discovery: Boolean = false,
    reviews: List<MissionReview> = emptyList(),
    reviewsSelected: Boolean = false,
    onSelectReviews: (Boolean) -> Unit = {},
    curated: Boolean = false,
    calendarHeader: CalendarDetailHeader? = null,
    suggestionIcon: (@Composable () -> Unit)? = null,
    nearbyImageRes: Int = UiR.drawable.kiri_place_card_image,
    modifier: Modifier = Modifier,
    showAppBar: Boolean = true,
    showTabs: Boolean = true,
    discoverySaveLabel: String? = null,
    discoverySaveEnabled: Boolean = true,
    onDiscoverySave: () -> Unit = {},
    // 상세를 아직 받지 못했거나 받지 못했을 때의 앱바 제목
    placeholderTitle: String = "미션 상세",
    // 다시 불러와도 결과가 같은 오류면 false 로 다시 시도 버튼을 숨긴다
    showRetry: Boolean = true,
) {
    val onBack = { onEvent(DetailUiEvent.BackClicked) }
    val onCustom = { onEvent(DetailUiEvent.CustomClicked) }
    val onRetry = { onEvent(DetailUiEvent.RetryClicked) }
    val onRecord = { onEvent(DetailUiEvent.RecordClicked) }
    val detail = state.detail
    Column(
        modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .testTag(if (discovery) "mission_preview_screen" else "mission_detail_screen")
            .verticalScroll(rememberScrollState())
    ) {
        if (detail == null) {
            if (showAppBar) KiriDetailAppBar(placeholderTitle, onBack)
            if (state.loading)
                CircularProgressIndicator(
                    Modifier.align(Alignment.CenterHorizontally).padding(32.dp)
                )
            else
                Column(Modifier.padding(18.dp)) {
                    Text(state.error ?: "미션을 찾을 수 없어요.", style = KiriDetailTypography.Body)
                    if (showRetry) KiriDetailButton("다시 시도", onRetry)
                }
        } else {
            DetailHeader(
                detail,
                onBack,
                onCustom,
                discovery,
                reviewsSelected,
                curated,
                onRecord,
                state.submittedMemory != null,
                state.memoriesSelected,
                calendarHeader,
                showAppBar,
            )
            if (discovery && discoverySaveLabel != null) {
                Column(Modifier.padding(horizontal = 18.dp)) {
                    KiriDetailButton(discoverySaveLabel, onDiscoverySave,
                        enabled = discoverySaveEnabled,
                        modifier = Modifier.testTag("preview_save"))
                    if (state.error != null) Text(state.error, color = KiriColor.Text.Primary,
                        style = KiriDetailTypography.Body)
                }
            }
            Spacer(Modifier.height(if (showTabs) 36.dp else 16.dp))
            if (showTabs)
                Row(
                    Modifier.align(Alignment.CenterHorizontally)
                        .background(KiriColor.Bg.Secondary, RoundedCornerShape(100.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (discovery) {
                        PreviewTab(
                            "추천 활동",
                            !reviewsSelected,
                            { onSelectReviews(false) },
                            "preview_activity_tab",
                        )
                        PreviewTab(
                            "리뷰집",
                            reviewsSelected,
                            { onSelectReviews(true) },
                            "preview_review_tab",
                        )
                    } else {
                        PreviewTab(
                            "끼리네 AI 활동",
                            !state.memoriesSelected,
                            { onEvent(DetailUiEvent.MemoriesSelected(false)) },
                            "mission_activity_tab",
                        )
                        PreviewTab(
                            "가족 추억",
                            state.memoriesSelected,
                            { onEvent(DetailUiEvent.MemoriesSelected(true)) },
                            "mission_memory_tab",
                        )
                    }
                }
            Spacer(Modifier.height(if (showTabs) 36.dp else 0.dp))
            Column(Modifier.padding(horizontal = 18.dp)) {
                if (!discovery && state.memoriesSelected) {
                    if (state.submittedMemory == null) {
                        // Figma 9898:17487 (KIRI/Empty State)
                        com.kiri.uikit.component.KiriEmptyState(
                            "아직 기록이 없어요!",
                            Modifier.testTag("family_memories_empty"),
                        )
                    } else {
                        Text(
                            "${detail.familyName.ifBlank { "우리 가족" }} 님의 한 줄",
                            style = KiriDetailTypography.Title,
                            color = KiriColor.Text.Strong,
                        )
                        Spacer(Modifier.height(16.dp))
                        MemoryPreviewCard(
                            state.submittedMemory,
                            detail.participants.firstOrNull()?.name ?: "나",
                            onRecord,
                        )
                    }
                } else if (discovery && reviewsSelected) {
                    if (reviews.isEmpty()) EmptyInfo("아직 등록된 리뷰가 없어요.")
                    Column(
                        Modifier.fillMaxWidth().testTag("preview_reviews"),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        reviews.forEach { MissionReviewCard(it) }
                    }
                } else {
                    SectionTitle(
                        detail.familyName.ifBlank { "우리 가족" },
                        if (discovery || calendarHeader != null) "의 맞춤 활동" else "의 필수 활동",
                    )
                    Spacer(Modifier.height(16.dp))
                    if (detail.activities.isEmpty()) EmptyInfo("아직 등록된 필수 활동이 없어요.")
                    detail.activities.forEachIndexed { index, activity ->
                        if (index > 0) Spacer(Modifier.height(16.dp))
                        KiriDetailSuggestionCard(
                            activity.title,
                            activity.description,
                            Modifier.fillMaxWidth(),
                            leadingIcon = suggestionIcon,
                        )
                    }
                    Spacer(Modifier.height(48.dp))
                    SectionTitle(
                        detail.familyName.ifBlank { "우리 가족" },
                        if (discovery || calendarHeader != null) "의 맞춤템" else "의 필수템",
                    )
                    Spacer(Modifier.height(16.dp))
                    if (detail.essentials.isEmpty()) EmptyInfo("일정에 맞는 준비물 정보가 아직 없어요.")
                    detail.essentials.chunked(2).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { item ->
                                KiriDetailSuggestionCard(
                                    item.title,
                                    item.description,
                                    Modifier.weight(1f),
                                    item = true,
                                    leadingIcon = suggestionIcon,
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(48.dp))
                    Text(
                        "${detail.title} 근처 놀거리추천",
                        style = KiriDetailTypography.Title,
                        color = KiriColor.Text.Strong,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = KiriColor.Text.Accent)) {
                                append(detail.familyName.ifBlank { "우리 가족" })
                            }
                            append("한테 맞는 놀거리를 추천해드려요!")
                        },
                        style = KiriDetailTypography.Small,
                        color = KiriColor.Text.Tertiary,
                    )
                    if (detail.conditions.isNotEmpty())
                        Text(
                            "컨디션에 맞게 추천 순서를 조정했어요.",
                            Modifier.padding(top = 8.dp),
                            style = KiriDetailTypography.Small,
                            color = KiriColor.Text.Accent,
                        )
                    Spacer(Modifier.height(16.dp))
                }
            }
            if (if (discovery) !reviewsSelected else !state.memoriesSelected) {
                if (state.nearby.isEmpty())
                    Box(Modifier.padding(horizontal = 18.dp)) { EmptyInfo("주변 추천 정보가 아직 없어요.") }
                else
                    Row(
                        Modifier.horizontalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        state.nearby.forEach { place ->
                            KiriPlaceCard(
                                place.title,
                                place.address,
                                place.description,
                                rating = null,
                                detailStyle = true,
                                imageRes = nearbyImageRes,
                            )
                        }
                    }
            }
            Spacer(Modifier.height(if (calendarHeader != null) 120.dp else 32.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeader(
    detail: MissionDetail,
    onBack: () -> Unit,
    onCustom: () -> Unit,
    discovery: Boolean = false,
    reviewsSelected: Boolean = false,
    curated: Boolean = false,
    onRecord: () -> Unit = {},
    memorySubmitted: Boolean = false,
    memoriesSelected: Boolean = false,
    calendarHeader: CalendarDetailHeader? = null,
    showAppBar: Boolean = true,
) {
    Column(Modifier.fillMaxWidth().background(KiriColor.Bg.Primary)) {
        if (showAppBar)
            KiriDetailAppBar(
                calendarHeader?.title
                    ?: if (memorySubmitted) "${detail.familyName.ifBlank { "우리 가족" }} 기록"
                    else if (discovery && reviewsSelected) "플레이스"
                    else
                        detail.date?.let { "${it.monthValue}월 ${it.dayOfMonth}일" }
                            ?: if (discovery) "플레이스" else "미션 상세",
                onBack,
            )
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 21.dp, bottom = 34.dp)) {
            val today = com.kiri.uikit.LocalKiriClock.current.kiriToday()
            val badge =
                calendarHeader?.badge
                    ?: when {
                        discovery -> if (curated) "끼리추천" else "추천 활동"
                        detail.completed -> "완료한 미션이에요!"
                        detail.date == today -> "약속 당일이에요!"
                        detail.date == null -> "이번 주 미션이에요!"
                        detail.date?.isBefore(today) == true -> "지난 약속이에요"
                        else -> "다가오는 약속이에요!"
                    }
            val neutralBadge =
                calendarHeader == null && (detail.completed || memoriesSelected || memorySubmitted)
            val calendarNeutral = calendarHeader?.neutralBadge == true
            Text(
                badge,
                Modifier.background(
                        if (neutralBadge || calendarNeutral) KiriColor.Bg.Elevated
                        // 캘린더 일정의 확정 배지 (Figma 9898:21887, kiri-bg-green-soft)
                        else KiriColor.Bg.GreenSoft,
                        RoundedCornerShape(5.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                style = KiriDetailTypography.Small,
                color =
                    if (calendarNeutral) KiriColor.Text.Secondary
                    else if (neutralBadge) KiriColor.Text.Primary
                    else KiriColor.Text.Accent,
            )
            Spacer(Modifier.height(11.dp))
            Text(detail.title, color = KiriColor.Text.Strong, style = KiriDetailTypography.Display)
            Spacer(Modifier.height(4.dp))
            if (calendarHeader?.collapseParticipants == true) {
                ParticipantCheckLine(detail)
            } else if (memorySubmitted) {
                Text(
                    buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                textDecoration =
                                    androidx.compose.ui.text.style.TextDecoration.Underline
                            )
                        ) {
                            append("인원확인")
                        }
                        append(
                            " I ${detail.date?.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")) ?: "날짜 정보 없음"}"
                        )
                    },
                    color = KiriColor.Text.Primary,
                    style = KiriDetailTypography.Body,
                )
            } else
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "참여상태  I",
                        color = KiriColor.Text.Primary,
                        style = KiriDetailTypography.Body,
                    )
                    if (detail.participants.isEmpty())
                        Text(
                            "참여 정보 없음",
                            color = KiriColor.Text.Secondary,
                            style = KiriDetailTypography.Body,
                        )
                    detail.participants.forEach { participant ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                participant.name,
                                color = KiriColor.Text.Primary,
                                style = KiriDetailTypography.Body,
                            )
                            if (participant.participating) {
                                if (discovery)
                                    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                        Image(
                                            painterResource(
                                                DetailR.drawable.detail_participant_check
                                            ),
                                            "참여",
                                            Modifier.size(22.dp),
                                        )
                                    }
                                else
                                    Image(
                                        painterResource(UiR.drawable.kiri_detail_check),
                                        "참여",
                                        Modifier.size(18.dp),
                                    )
                            } else
                                Text(
                                    "미참여",
                                    color = KiriColor.Text.Tertiary,
                                    style = KiriDetailTypography.Small,
                                )
                        }
                    }
                }
            Spacer(Modifier.height(22.dp))
            Column(
                Modifier.fillMaxWidth()
                    .background(KiriColor.Bg.Secondary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 26.dp, vertical = 17.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InfoText("시작 시간   I   ${detail.startTime ?: "미정"}")
                InfoText("장소   I   ${detail.place ?: "미정"}")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    InfoText("예상 걸음수 I ${detail.stepsLabel}")
                    InfoText("활동시간 I ${detail.durationLabel}")
                }
            }
            if (!discovery && (calendarHeader == null || calendarHeader.showCustom)) {
                Spacer(Modifier.height(16.dp))
                val canRecord = detail.completed || detail.date?.let { !it.isAfter(today) } == true
                val showMemoryAction =
                    calendarHeader == null &&
                        (memoriesSelected || detail.completed || memorySubmitted)
                KiriDetailButton(
                    text = if (showMemoryAction) "오늘의 추억 기록하러가기" else "컨디션 조정하러가기",
                    onClick = if (showMemoryAction) onRecord else onCustom,
                    accent = showMemoryAction && canRecord && !memorySubmitted,
                    enabled = if (showMemoryAction) canRecord else !detail.completed,
                    modifier =
                        Modifier.testTag(
                            if (showMemoryAction) "open_memory_editor" else "open_custom"
                        ),
                    softAccent = calendarHeader != null,
                )
            }
        }
    }
}

// "인원확인 I 날짜". 인원확인을 누르면 화면을 어둡게 덮고 그 아래에 참여자 말풍선을 띄운다. 바깥을 누르면 닫힌다
@Composable
private fun ParticipantCheckLine(detail: MissionDetail) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var anchor by remember { mutableStateOf(IntOffset.Zero) }
    val density = LocalDensity.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "인원확인",
            Modifier.onGloballyPositioned {
                    val bounds = it.boundsInWindow()
                    anchor = IntOffset(bounds.left.roundToInt(), bounds.bottom.roundToInt())
                }
                .clickable(onClickLabel = "참여 인원 보기") { expanded = true }
                .testTag("participant_check"),
            color = KiriColor.Text.Primary,
            style = KiriDetailTypography.Body.copy(textDecoration = TextDecoration.Underline),
        )
        Text(
            "  I  ${detail.date?.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")) ?: "날짜 정보 없음"}",
            color = KiriColor.Text.Primary,
            style = KiriDetailTypography.Body,
        )
    }
    if (expanded)
        Popup(
            popupPositionProvider = WindowOriginPositionProvider,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true),
        ) {
            Box(
                Modifier.fillMaxSize()
                    .background(KiriColor.Bg.Base.copy(alpha = .7f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "닫기",
                    ) {
                        expanded = false
                    }
                    .testTag("participant_check_dim")
            ) {
                // Figma 에서 말풍선은 인원확인 글자보다 2dp 오른쪽, 꼬리가 글자 아래에 9dp 겹치도록 놓인다
                val offset = with(density) { IntOffset(2.dp.roundToPx(), -9.dp.roundToPx()) }
                KiriParticipantTooltip(
                    detail.participants.mapIndexed { index, participant ->
                        KiriParticipantStatus(
                            index.toString(),
                            participant.name,
                            participant.participating,
                            if (participant.participating) "수락" else "미수락",
                        )
                    },
                    Modifier.offset { anchor + offset }
                        .widthIn(max = 340.dp)
                        .testTag("participant_check_tooltip"),
                    showStatus = false,
                )
            }
        }
}

// 말풍선 팝업을 창 왼쪽 위에 붙여 화면 전체를 덮게 한다
private object WindowOriginPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ) = IntOffset.Zero
}

@Composable
private fun InfoText(text: String) {
    Text(text, color = KiriColor.Text.Primary, style = KiriDetailTypography.Body)
}

@Composable
private fun EmptyInfo(text: String) {
    Text(text, color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small)
}

@Composable
private fun SectionTitle(family: String, suffix: String) {
    Text(
        buildAnnotatedString {
            append("오늘 ")
            withStyle(SpanStyle(color = KiriColor.Text.Accent)) { append(family) }
            append(suffix)
        },
        color = KiriColor.Text.Strong,
        style = KiriDetailTypography.Title.copy(lineHeight = 22.sp),
    )
}
