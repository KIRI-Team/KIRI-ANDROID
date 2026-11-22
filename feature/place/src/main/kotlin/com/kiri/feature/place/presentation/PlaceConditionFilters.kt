package com.kiri.feature.place.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.domain.place.PlaceFilters
import com.kiri.feature.place.R
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun PlaceConditionFilters(filters: PlaceFilters, onSection: (Int) -> Unit) {
    val defaults = PlaceFilters()
    val date = filters.dateEpochDay?.let { day ->
        LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("M. d (E)", Locale.KOREAN)) +
            " · ${formatFilterTime(filters.minutesOfDay)} · ${filters.people}명"
    }
    val region = filters.regions.takeIf { it.isNotEmpty() }?.joinToString(", ")
    val distance = filters.distanceKm?.let { "${it}km 이내" }
    val price = if (filters.minPrice != defaults.minPrice || filters.maxPrice != defaults.maxPrice) {
        String.format(Locale.KOREAN, "%,d~%,d원", filters.minPrice, filters.maxPrice)
    } else null
    val active = filters.activeCount > 0
    val selectedBackground = KiriColor.Primary.Default.copy(alpha = .2f)

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 16.dp).testTag("place_condition_filters"),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = { onSection(0) },
            modifier = Modifier.size(36.dp).testTag("place_filters").semantics { selected = active },
            color = if (active) selectedBackground else KiriColor.Bg.Tertiary,
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painterResource(R.drawable.kiri_filter_action),
                    contentDescription = "상세 필터",
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(if (active) KiriColor.Text.Accent else KiriColor.Text.Secondary),
                )
            }
        }
        listOf("날짜" to date, "지역" to region, "거리" to distance, "가격" to price)
            .forEachIndexed { index, (label, value) ->
                val color = if (value != null) KiriColor.Text.Accent else KiriColor.Text.Secondary
                Surface(
                    onClick = { onSection(index) },
                    modifier = Modifier.height(36.dp).testTag("place_condition_$index")
                        .semantics { selected = value != null },
                    color = if (value != null) selectedBackground else KiriColor.Bg.Tertiary,
                    shape = CircleShape,
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (value == null) label else "$label · $value",
                            style = KiriDetailTypography.Small.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
                            color = color,
                            maxLines = 1,
                        )
                        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                            Image(
                                painterResource(R.drawable.kiri_filter_back),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).rotate(-90f),
                                colorFilter = ColorFilter.tint(color),
                            )
                        }
                    }
                }
            }
    }
}
