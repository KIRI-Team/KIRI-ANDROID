package com.kiri.feature.place.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kiri.domain.place.PlaceFilters
import com.kiri.feature.place.R
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

internal val PlaceFiltersSaver = listSaver<PlaceFilters, Any>(
    save = { listOf(it.regions.joinToString("|"), it.distanceKm ?: -1, it.dateEpochDay ?: Long.MIN_VALUE,
        it.minutesOfDay, it.people, it.minPrice, it.maxPrice) },
    restore = { PlaceFilters((it[0] as String).split('|').filter(String::isNotEmpty).toSet(),
        (it[1] as Int).takeIf { v -> v > 0 }, (it[2] as Long).takeIf { v -> v != Long.MIN_VALUE },
        it[3] as Int, it[4] as Int, it[5] as Int, it[6] as Int) },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaceFilterSheet(applied: PlaceFilters, onDismiss: () -> Unit, onApply: (PlaceFilters) -> Unit) {
    var draft by rememberSaveable(stateSaver = PlaceFiltersSaver) { mutableStateOf(applied) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    val scroll = rememberLazyListState()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = KiriColor.Bg.Primary, dragHandle = null, modifier = Modifier.testTag("place_filter_sheet")) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("place_filter_close")) {
                    Text("×", style = KiriDetailTypography.Title, color = KiriColor.Text.Secondary)
                }
            }
            Row(Modifier.padding(horizontal = 18.dp)) {
                listOf("날짜·인원", "지역", "거리", "가격").forEachIndexed { index, label ->
                    Column(Modifier.weight(if (index == 0) 1.5f else 1f)) {
                        TextButton(onClick = { selectedSection = index; scope.launch { scroll.animateScrollToItem(index) } },
                            modifier = Modifier.fillMaxWidth().testTag("filter_section_$index")) {
                            Text(label, color = if (selectedSection == index) KiriColor.Text.Primary else KiriColor.Text.Secondary,
                                style = KiriDetailTypography.Body)
                        }
                        HorizontalDivider(thickness = if (selectedSection == index) 2.dp else 1.dp,
                            color = if (selectedSection == index) KiriColor.Text.Primary else KiriColor.Bg.Elevated)
                    }
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("place_filter_content"), state = scroll) {
                item {
                    Surface(onClick = { pickingDate = true }, shape = RoundedCornerShape(8.dp),
                        color = KiriColor.Bg.Primary, border = androidx.compose.foundation.BorderStroke(1.dp, KiriColor.Bg.Tertiary),
                        modifier = Modifier.padding(18.dp).fillMaxWidth().heightIn(min = 48.dp).testTag("filter_date")) {
                        Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.kiri_filter_calendar), null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(draft.dateEpochDay?.let { day ->
                                LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("M. d (E)", Locale.KOREAN)) +
                                    " · ${formatFilterTime(draft.minutesOfDay)} · ${draft.people}명"
                            } ?: "날짜·인원·시간", Modifier.weight(1f), style = KiriDetailTypography.Small, color = KiriColor.Text.Primary)
                            if (draft.dateEpochDay != null) IconButton(onClick = { draft = draft.copy(dateEpochDay = null) }, modifier = Modifier.testTag("filter_date_clear")) {
                                Image(painterResource(R.drawable.kiri_filter_clear), "날짜 선택 해제", Modifier.size(18.dp))
                            } else Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                Image(painterResource(R.drawable.kiri_filter_back), null, Modifier.size(12.dp).rotate(180f))
                            }
                        }
                    }
                    HorizontalDivider(thickness = 6.dp, color = KiriColor.Bg.Secondary)
                }
                item {
                    Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 40.dp)) {
                        FilterHeading("지역")
                        Spacer(Modifier.height(12.dp))
                        listOf("전체", "서울", "인천", "경기", "대전", "세종", "충청", "대구", "경상", "울산", "부산", "광주", "전라", "강원", "제주").chunked(5).forEach { row ->
                            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { region -> FilterChip(region, if (region == "전체") draft.regions.isEmpty() else region in draft.regions, {
                                    draft = draft.copy(regions = if (region == "전체") emptySet() else if (region in draft.regions) draft.regions - region else draft.regions + region)
                                }, "filter_region_$region", Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 48.dp)) {
                        FilterHeading("거리")
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 3, 10).forEach { km -> FilterChip("${km}km 이내", draft.distanceKm == km,
                                { draft = draft.copy(distanceKm = km.takeUnless { draft.distanceKm == km }) }, "filter_distance_${km}km 이내") }
                        }
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
                        FilterHeading("가격")
                        Text("1인 기준 요금", style = KiriDetailTypography.Body, color = KiriColor.Text.Tertiary)
                        RangeSlider(value = draft.minPrice.toFloat()..draft.maxPrice.toFloat(),
                            onValueChange = { draft = draft.copy(minPrice = it.start.toInt(), maxPrice = it.endInclusive.toInt()) },
                            valueRange = 0f..1_000_000f, modifier = Modifier.testTag("filter_price"),
                            startThumb = { Image(painterResource(R.drawable.kiri_filter_thumb_selected), null, Modifier.size(18.dp)) },
                            endThumb = { Image(painterResource(R.drawable.kiri_filter_thumb_selected), null, Modifier.size(18.dp)) },
                            colors = SliderDefaults.colors(activeTrackColor = KiriColor.Primary.Default, inactiveTrackColor = KiriColor.Bg.Secondary))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${String.format(Locale.KOREAN, "%,d", draft.minPrice)}원", Modifier.testTag("filter_price_min"), color = KiriColor.Text.Tertiary, style = KiriDetailTypography.Small)
                            Text("${String.format(Locale.KOREAN, "%,d", draft.maxPrice)}원", Modifier.testTag("filter_price_max"), color = KiriColor.Text.Tertiary, style = KiriDetailTypography.Small)
                        }
                    }
                }
            }
            FilterActions({ draft = PlaceFilters() }, { onApply(draft) })
        }
    }
    if (pickingDate) PlaceDatePicker(draft, { pickingDate = false }) { draft = it; pickingDate = false }
}

@Composable internal fun FilterHeading(text: String) {
    Text(text, style = KiriDetailTypography.Title, color = KiriColor.Text.Strong)
}

@Composable internal fun FilterActions(onReset: () -> Unit, onApply: () -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KiriButton(onClick = onReset, text = "초기화", tone = KiriButtonTone.Neutral, modifier = Modifier.weight(1f), testTag = "filter_reset")
        KiriButton(onClick = onApply, text = "적용하기", modifier = Modifier.weight(1f), enabled = enabled, testTag = "filter_apply")
    }
}

@Composable internal fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit, tag: String, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, shape = RoundedCornerShape(100.dp),
        color = if (selected) KiriColor.Primary.Default.copy(alpha = .2f) else KiriColor.Bg.Tertiary,
        modifier = modifier.heightIn(min = 36.dp).testTag(tag).semantics { this.selected = selected }) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(label, style = KiriDetailTypography.Small, maxLines = 1,
                color = if (selected) KiriColor.Text.Accent else KiriColor.Text.Secondary)
        }
    }
}

internal fun formatFilterTime(minutes: Int): String = String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)
