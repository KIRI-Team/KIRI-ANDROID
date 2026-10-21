package com.kiri.uikit.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.kiri.uikit.KiriTypography
import com.kiri.uikit.R
import com.kiri.uikit.color.KiriColor

@Composable
fun KiriFormSelectRow(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(onClick = onClick, enabled = enabled, color = KiriColor.Bg.Primary, shape = RoundedCornerShape(16.dp), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.heightIn(min = 85.dp).padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = KiriTypography.BodyMedium, color = KiriColor.Text.Secondary)
                Text(value, style = KiriTypography.Value, color = KiriColor.Text.Strong)
            }
            Image(painterResource(R.drawable.kiri_form_back), null, Modifier.size(24.dp).rotate(180f))
        }
    }
}

@Composable
fun KiriFamilySelectionCard(name: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(8.dp),
        color = if (selected) KiriColor.Bg.GreenSoft else KiriColor.Bg.Primary,
        modifier = modifier.semantics { this.selected = selected; role = Role.Checkbox }) {
        Column(Modifier.heightIn(min = 106.333.dp).padding(top = 16.dp, bottom = 8.dp, start = 12.dp, end = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            val tint = if (selected) KiriColor.Bg.Green else KiriColor.Bg.Primary
            // Figma uses the original asset as a luminance mask with plus-lighter blending.
            val mask = ColorMatrix(floatArrayOf(0f,0f,0f,0f,tint.red*255, 0f,0f,0f,0f,tint.green*255,
                0f,0f,0f,0f,tint.blue*255, .2126f,.7152f,.0722f,1f,-255f))
            Image(painterResource(if (selected) R.drawable.kiri_family_selected else R.drawable.kiri_family_unselected), null,
                Modifier.size(40.dp).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; blendMode = BlendMode.Plus }, colorFilter = ColorFilter.colorMatrix(mask))
            Text(name, style = KiriTypography.BodyLarge, color = if (selected) KiriColor.Text.Accent else KiriColor.Text.Primary)
        }
    }
}

data class KiriParticipantStatus(val id: String, val name: String, val accepted: Boolean, val status: String)

/** Horizontal scrolling keeps long nicknames and large families readable. */
@Composable
fun KiriParticipantTooltip(participants: List<KiriParticipantStatus>, modifier: Modifier = Modifier) {
    Box(modifier) {
    Image(painterResource(R.drawable.kiri_participant_pointer), null, Modifier.offset(x = 11.95.dp).size(38.105.dp, 28.476.dp))
    Row(Modifier.padding(top = 16.dp).background(KiriColor.Bg.Secondary, RoundedCornerShape(16.dp)).horizontalScroll(rememberScrollState()).padding(horizontal = 21.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        participants.forEach { participant ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics(mergeDescendants = true) { stateDescription = participant.status }) {
                Image(painterResource(if (participant.accepted) R.drawable.kiri_participant_accepted else R.drawable.kiri_participant_pending), null, Modifier.size(24.dp))
                Text(participant.name, style = KiriTypography.TitleMedium, color = if (participant.accepted) KiriColor.Text.Primary else KiriColor.Text.Disabled)
                Text(participant.status, style = KiriTypography.BodySmall, color = KiriColor.Text.Secondary)
            }
        }
    }
    }
}

@Composable
fun KiriParticipantStatusButton(participants: List<KiriParticipantStatus>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val offset = with(LocalDensity.current) { 40.dp.roundToPx() }
    Box(modifier) {
        TextButton({ expanded = !expanded }) { Text("인원확인", style = KiriTypography.BodyMedium, color = KiriColor.Text.Primary) }
        if (expanded) Popup(alignment = Alignment.TopStart, offset = IntOffset(0, offset), onDismissRequest = { expanded = false }, properties = PopupProperties(focusable = true)) {
            KiriParticipantTooltip(participants, Modifier.widthIn(max = 340.dp))
        }
    }
}
