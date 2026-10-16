package com.kiri.feature.place.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.feature.place.R
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Local UI selections; the repository does not yet support detailed filtering. */
internal data class PlaceFilters(
    val region: String = "서울특별시", val distance: String = "1km 이내",
    val dateMillis: Long? = null, val hour: Int = 14, val minute: Int = 50,
    val minPrice: Float = 0f, val maxPrice: Float = 1_000_000f,
) {
    companion object {
        val Saver = listSaver<PlaceFilters, Any>(
            save = { listOf(it.region, it.distance, it.dateMillis ?: -1L, it.hour, it.minute, it.minPrice, it.maxPrice) },
            restore = { PlaceFilters(it[0] as String, it[1] as String, (it[2] as Long).takeIf { value -> value >= 0 }, it[3] as Int, it[4] as Int, it[5] as Float, it[6] as Float) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaceFilterSheet(applied: PlaceFilters, onDismiss: () -> Unit, onApply: (PlaceFilters) -> Unit) {
    var draft by rememberSaveable(stateSaver = PlaceFilters.Saver) { mutableStateOf(applied) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    val heading = KiriDetailTypography.Title.copy(fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-.8).sp)
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), containerColor = KiriColor.Bg.Primary,
        dragHandle = null, modifier = Modifier.testTag("place_filter_sheet"),
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.94f)) {
            Box(Modifier.fillMaxWidth().height(95.dp)) {
                Text("필터", Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 48.dp), style = heading, color = KiriColor.Text.Strong)
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 16.dp).testTag("place_filter_close")) {
                    Box(Modifier.size(24.dp).semantics { contentDescription = "필터 닫기" }, contentAlignment = Alignment.Center) {
                        Box(Modifier.rotate(45f).size(width = 3.54.dp, height = 23.227.dp).background(KiriColor.Bg.Elevated, RoundedCornerShape(100.dp)))
                        Box(Modifier.rotate(-45f).size(width = 3.54.dp, height = 23.227.dp).background(KiriColor.Bg.Elevated, RoundedCornerShape(100.dp)))
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).testTag("place_filter_content")) {
                Spacer(Modifier.height(16.dp))
                Text("지역", style = heading, color = KiriColor.Text.Strong)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("서울특별시", "경기", "인천").forEach { region ->
                        FilterChip(region, draft.region == region, { draft = draft.copy(region = region) }, "filter_region_$region")
                    }
                }
                Spacer(Modifier.height(91.dp))
                Text("거리", style = heading, color = KiriColor.Text.Strong)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1km 이내", "3km 이내", "10km 이내").forEach { distance ->
                        FilterChip(distance, draft.distance == distance, { draft = draft.copy(distance = distance) }, "filter_distance_$distance")
                    }
                }
                Spacer(Modifier.height(48.dp))
                Text("날짜", style = heading, color = KiriColor.Text.Strong)
                Spacer(Modifier.height(12.dp))
                Surface(onClick = { pickingDate = true }, color = KiriColor.Bg.Primary, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 85.dp).testTag("filter_date")) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("날짜 선택", style = KiriDetailTypography.Small, color = KiriColor.Text.Secondary)
                            val date = draft.dateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ofPattern("yy.MM.dd(E)", Locale.KOREAN)) }
                            Text(if (date == null) "날짜·시간 선택" else "$date · ${draft.hour}시 ${draft.minute.toString().padStart(2, '0')}분",
                                style = heading.copy(lineHeight = 30.sp), color = KiriColor.Text.Strong)
                        }
                        Image(painterResource(R.drawable.kiri_filter_back), null, Modifier.size(24.dp).rotate(180f))
                    }
                }
                Spacer(Modifier.height(48.dp))
                Text("가격", style = heading, color = KiriColor.Text.Strong)
                Text("1인 기준 요금", style = KiriDetailTypography.Body, color = KiriColor.Text.Tertiary)
                val priceSelected = draft.minPrice != 0f || draft.maxPrice != 1_000_000f
                val priceTrackColor = if (priceSelected) KiriColor.Primary.Default else KiriColor.Bg.Secondary
                RangeSlider(value = draft.minPrice..draft.maxPrice,
                    onValueChange = { draft = draft.copy(minPrice = it.start, maxPrice = it.endInclusive) },
                    valueRange = 0f..1_000_000f, modifier = Modifier.fillMaxWidth().testTag("filter_price"),
                    startThumb = { Image(painterResource(if (priceSelected) R.drawable.kiri_filter_thumb_selected else R.drawable.kiri_filter_thumb), null, Modifier.size(18.dp)) },
                    endThumb = { Image(painterResource(if (priceSelected) R.drawable.kiri_filter_thumb_selected else R.drawable.kiri_filter_thumb), null, Modifier.size(18.dp)) },
                    colors = SliderDefaults.colors(activeTrackColor = priceTrackColor, inactiveTrackColor = KiriColor.Bg.Secondary),
                    track = { range -> SliderDefaults.Track(rangeSliderState = range, modifier = Modifier.height(8.dp),
                        colors = SliderDefaults.colors(activeTrackColor = priceTrackColor, inactiveTrackColor = KiriColor.Bg.Secondary), drawStopIndicator = null, thumbTrackGapSize = 0.dp) },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val style = KiriDetailTypography.Small.copy(fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = (-.48).sp, fontWeight = FontWeight.Medium)
                    Text("${String.format(Locale.KOREAN, "%,d", draft.minPrice.toInt())}원", style = style, color = KiriColor.Text.Tertiary, modifier = Modifier.testTag("filter_price_min"))
                    Text("${String.format(Locale.KOREAN, "%,d", draft.maxPrice.toInt())}원", style = style, color = KiriColor.Text.Tertiary, modifier = Modifier.testTag("filter_price_max"))
                }
                Spacer(Modifier.height(24.dp))
            }
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                KiriButton(onClick = { draft = PlaceFilters() }, text = "초기화", tone = KiriButtonTone.Neutral,
                    modifier = Modifier.weight(130f), testTag = "filter_reset", leadingIcon = {
                        Image(painterResource(R.drawable.kiri_filter_refresh), null, Modifier.size(18.dp))
                    })
                KiriButton(onClick = { onApply(draft) }, text = "적용하기", modifier = Modifier.weight(215f), testTag = "filter_apply")
            }
        }
    }
    if (pickingDate) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = draft.dateMillis)
        DatePickerDialog(onDismissRequest = { pickingDate = false }, confirmButton = {
            TextButton(onClick = { draft = draft.copy(dateMillis = dateState.selectedDateMillis); pickingDate = false; pickingTime = true }, enabled = dateState.selectedDateMillis != null) { Text("시간 선택") }
        }, dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("취소") } }) { DatePicker(dateState) }
    }
    if (pickingTime) {
        val timeState = rememberTimePickerState(initialHour = draft.hour, initialMinute = draft.minute, is24Hour = true)
        AlertDialog(onDismissRequest = { pickingTime = false }, title = { Text("시간 선택") }, text = { TimePicker(timeState) },
            confirmButton = { TextButton(onClick = { draft = draft.copy(hour = timeState.hour, minute = timeState.minute); pickingTime = false }) { Text("확인") } },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("취소") } })
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit, tag: String) {
    Surface(onClick = onClick, shape = RoundedCornerShape(100.dp),
        color = if (selected) KiriColor.Primary.Default.copy(alpha = .2f) else KiriColor.Bg.Tertiary,
        modifier = Modifier.height(36.dp).testTag(tag).semantics { this.selected = selected }) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(label, style = KiriDetailTypography.Small.copy(fontWeight = FontWeight.Medium, lineHeight = 20.sp),
                color = if (selected) KiriColor.Text.Accent else KiriColor.Text.Secondary)
        }
    }
}
