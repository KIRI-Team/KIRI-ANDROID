package com.kiri.uikit.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiri.uikit.R
import com.kiri.uikit.color.KiriColor

val KiriPretendard = FontFamily(Font(R.font.pretendard_regular), Font(R.font.pretendard_medium, FontWeight.Medium))

object KiriDetailTypography {
    val Body = TextStyle(fontFamily = KiriPretendard, fontSize = 16.sp, lineHeight = 26.sp, letterSpacing = (-0.64).sp)
    val Small = TextStyle(fontFamily = KiriPretendard, fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = (-0.56).sp)
    val Title = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 27.sp, letterSpacing = (-0.72).sp)
    val Display = TextStyle(fontFamily = KiriPretendard, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 40.sp, letterSpacing = (-0.56).sp)
}

/** Figma 8413:5560 / 8448:17662. Insets are owned by the host scaffold. */
@Composable
fun KiriAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backEnabled: Boolean = true,
    trailingIconSlot: (@Composable () -> Unit)? = null,
) {
    Box(modifier.fillMaxWidth().height(62.dp)) {
        Text(title, Modifier.align(Alignment.Center).padding(horizontal = 52.dp), color = KiriColor.Text.Strong, style = KiriDetailTypography.Title)
        if (onBack != null) {
            KiriLeadingIconSlot(onBack, Modifier.align(Alignment.CenterStart), backEnabled)
        }
        if (trailingIconSlot != null) {
            Box(Modifier.align(Alignment.CenterEnd).size(52.dp), contentAlignment = Alignment.Center) {
                trailingIconSlot()
            }
        }
    }
}

@Composable
fun KiriLeadingIconSlot(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(52.dp)) {
        // Supplied SVG already contains the centered back icon within the 52px slot.
        Image(painterResource(R.drawable.kiri_leading_icon_slot), "뒤로", Modifier.size(52.dp))
    }
}

@Composable
fun KiriDetailAppBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    KiriAppBar(title, modifier, onBack, enabled)
}

/** Figma 8431:8357 / 8448:17924. Keep the supplied colors even for a deferred action. */
@Composable
fun KiriDetailButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Boolean = false, enabled: Boolean = true, custom: Boolean = false) {
    val background = if (accent) KiriColor.Primary.Default else if (custom) KiriColor.Bg.Secondary else KiriColor.Bg.Tertiary
    val foreground = if (accent) KiriColor.Text.Inverse else KiriColor.Text.Strong
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = if (custom) 58.dp else 48.dp),
        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = foreground, disabledContainerColor = background, disabledContentColor = foreground),
    ) { Text(text, style = KiriDetailTypography.Title, textAlign = TextAlign.Center) }
}

/** Figma 8448:17522 / 8448:17523, 151 x 149.5, gap 8, icon 32. */
@Composable
fun KiriConditionCard(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Column(
        modifier.heightIn(min = 149.5.dp)
            .background(if (selected) KiriColor.Bg.GreenSoft else KiriColor.Bg.Secondary, RoundedCornerShape(8.dp))
            .toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 12.dp, vertical = 30.75.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(if (selected) R.drawable.kiri_detail_condition_selected else R.drawable.kiri_detail_condition), null, Modifier.size(32.dp))
        Text(label, style = KiriDetailTypography.Body.copy(fontWeight = FontWeight.Medium, lineHeight = 24.sp), color = if (selected) KiriColor.Text.Accent else KiriColor.Text.Primary, textAlign = TextAlign.Center)
    }
}

@Composable
fun KiriDetailSuggestionCard(title: String, description: String, modifier: Modifier = Modifier, item: Boolean = false) {
    KiriContentCard(modifier) {
        Column(
            Modifier.heightIn(min = 110.dp)
                // The 110px activity instance centers 77px of content (16.5px each side).
                .padding(horizontal = if (item) 18.dp else 30.dp, vertical = if (item) 18.dp else 16.5.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.kiri_umbrella_active), null, Modifier.size(width = 19.dp, height = 21.dp))
                }
                Text(title, color = KiriColor.Text.Strong, style = if (item) KiriDetailTypography.Title.copy(lineHeight = 22.sp) else KiriDetailTypography.Title)
            }
            Text(description, color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small)
        }
    }
}
