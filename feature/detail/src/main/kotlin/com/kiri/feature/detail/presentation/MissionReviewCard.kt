package com.kiri.feature.detail.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.domain.mission.MissionReview
import com.kiri.feature.detail.R
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography

@Composable
internal fun PreviewTab(label: String, selected: Boolean, onClick: () -> Unit, tag: String) {
    Text(label, Modifier.background(if (selected) KiriColor.Bg.Tertiary else KiriColor.Bg.Secondary, RoundedCornerShape(30.dp))
        .selectable(selected, onClick = onClick).testTag(tag).padding(horizontal = 20.dp, vertical = 11.dp),
        color = if (selected) KiriColor.Text.Strong else KiriColor.Text.Disabled, style = KiriDetailTypography.Title.copy(lineHeight = 22.sp))
}

@Composable
internal fun MissionReviewCard(review: MissionReview) {
    Column(Modifier.fillMaxWidth().background(KiriColor.Bg.Secondary, RoundedCornerShape(16.dp)).padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 23.dp).testTag("mission_review_${review.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Image(painterResource(R.drawable.detail_review_avatar), null, Modifier.size(36.dp))
            Text(review.author, style = KiriDetailTypography.Body, color = KiriColor.Text.Primary)
        }
        Spacer(Modifier.height(19.dp))
        Box(Modifier.fillMaxWidth().height(185.dp).background(KiriColor.Bg.Elevated, RoundedCornerShape(4.dp)))
        Spacer(Modifier.height(18.dp))
        Text(review.text, style = KiriDetailTypography.Body, color = KiriColor.Text.Primary)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.detail_review_heart), "좋아요", Modifier.size(width = 20.2477.dp, height = 18.1389.dp))
            }
            Text(review.likes.toString(), style = KiriDetailTypography.Body, color = KiriColor.Text.Secondary)
        }
    }
}
