package com.kiri.feature.place.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.domain.place.PlaceMission
import com.kiri.feature.place.R
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriAsyncImage
import com.kiri.uikit.component.KiriDetailTypography

@Composable
internal fun PlaceKiriPickSection(picks: List<PlaceMission>, onPick: (String, Long) -> Unit) {
    HorizontalPager(
        state = rememberPagerState { picks.size },
        modifier = Modifier.fillMaxWidth().padding(top = 17.dp).testTag("place_kiri_picks"),
        contentPadding = PaddingValues(horizontal = 18.dp),
        pageSpacing = 12.dp,
        key = { picks[it].id },
    ) { page ->
        val mission = picks[page]
        val pick = requireNotNull(mission.kiriPick)
        Box(
            Modifier.fillMaxWidth()
                .heightIn(min = 120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KiriColor.Bg.Elevated)
                .clickable(role = Role.Button, onClickLabel = "미션 상세 보기") { onPick(mission.id, pick.id) }
                .testTag("place_kiri_pick_${mission.id}"),
        ) {
            (pick.imageUrl ?: mission.imageUrl)?.let { url ->
                KiriAsyncImage(url, null, Modifier.matchParentSize())
            }
            Box(
                Modifier.matchParentSize().background(
                    Brush.horizontalGradient(
                        listOf(KiriColor.Bg.Base.copy(alpha = .8f), KiriColor.Bg.Base.copy(alpha = .48f)),
                    ),
                ),
            )
            Row(
                Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KiriPickLabel()
                    Column {
                        Text(
                            pick.headline,
                            color = KiriColor.Text.Strong,
                            style = KiriDetailTypography.Title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            pick.description?.takeIf { it.isNotBlank() } ?: "미션 상세 보기 →",
                            color = KiriColor.Text.Primary,
                            style = KiriDetailTypography.Small,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    "광고",
                    modifier = Modifier.background(Color.White.copy(alpha = .2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    color = KiriColor.Text.Secondary,
                    style = KiriDetailTypography.Small.copy(fontSize = 12.sp, lineHeight = 18.sp),
                )
            }
        }
    }
}

@Composable
internal fun EmptyKiriPickSection() {
    Column(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 17.dp)
            .clip(RoundedCornerShape(8.dp)).background(KiriColor.Bg.Primary)
            .padding(horizontal = 16.dp, vertical = 12.dp).testTag("place_kiri_picks_empty"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        KiriPickLabel()
        Text("현재 공개된 끼리픽이 없어요.", color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small)
    }
}

@Composable
private fun KiriPickLabel() {
    Row(
        Modifier.clip(RoundedCornerShape(4.dp))
            .background(KiriColor.Bg.Base)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.place_kiri_pick_crown),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            "오늘의 끼리픽",
            color = KiriColor.Text.Accent,
            style = KiriDetailTypography.Small.copy(
                fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 18.sp,
            ),
        )
    }
}
