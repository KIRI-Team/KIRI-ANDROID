package com.kiri.uikit.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.PopupPositionProvider
import com.kiri.uikit.R
import com.kiri.uikit.color.KiriColor

data class KiriAvatarItem(
    val id: String,
    val label: String,
    val answered: Boolean,
    val enabled: Boolean = true,
    val isCurrentUser: Boolean = false,
    val role: String? = null,
)

@Composable
fun KiriMemberAvatarGroup(
    members: List<KiriAvatarItem>,
    onMemberClick: (KiriAvatarItem) -> Unit,
    onPositioned: (String, LayoutCoordinates) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        members.forEach { member ->
            Column(
                modifier = Modifier
                    .width(if (member.isCurrentUser) 46.dp else 44.dp)
                    .testTag("home_member_${member.id}")
                    .clickable(enabled = member.enabled) { onMemberClick(member) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                KiriMemberAvatar(
                    answered = member.answered,
                    role = member.role,
                    showAddBadge = member.isCurrentUser,
                    modifier = Modifier.testTag("home_avatar_${member.id}").onGloballyPositioned { onPositioned(member.id, it) },
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = member.label,
                    color = KiriColor.Text.Primary,
                    style = KiriDetailTypography.Body.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun KiriMemberAvatar(answered: Boolean, role: String? = null, showAddBadge: Boolean, modifier: Modifier = Modifier) {
    val character = when (role) {
        "엄마", "MOTHER" -> R.drawable.kiri_avatar_mother
        "아빠", "FATHER" -> R.drawable.kiri_avatar_father
        "딸", "DAUGHTER" -> R.drawable.kiri_avatar_daughter
        "아들", "SON" -> R.drawable.kiri_avatar_son
        else -> R.drawable.kiri_avatar_son
    }
    val characterSize = when (role) { "엄마", "MOTHER" -> 28.dp; "아빠", "FATHER" -> 24.dp; else -> 27.dp }
    Box(
        modifier
            .size(44.dp)
            .then(if (showAddBadge && !answered) Modifier else Modifier.border(1.dp, if (answered) KiriColor.Text.Accent else KiriColor.Border.Default, CircleShape))
            .semantics { stateDescription = if (answered) "답변 완료" else "미답변" },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(character),
            contentDescription = role,
            modifier = Modifier.size(40.dp).background(KiriColor.Bg.Secondary, CircleShape).padding((40.dp - characterSize) / 2),
            colorFilter = if (answered) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            alpha = if (answered) 1f else 0.6f,
        )
        if (showAddBadge && !answered) Image(painterResource(R.drawable.kiri_my_answer_ring), null, Modifier.size(44.dp))
    }
}

@Composable
fun KiriTooltip(
    title: String,
    answer: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    anchor: IntRect,
    onDismiss: () -> Unit = {},
) {
    val density = LocalDensity.current
    val width = (LocalConfiguration.current.screenWidthDp.dp - 36.dp).coerceAtMost(303.dp)
    var placement by remember(anchor) { mutableStateOf(KiriTooltipPlacement(IntOffset.Zero, 72f, true)) }
    val provider = remember(anchor, density) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: androidx.compose.ui.unit.IntSize, layoutDirection: androidx.compose.ui.unit.LayoutDirection, popupContentSize: androidx.compose.ui.unit.IntSize): IntOffset {
                return calculateKiriTooltipPlacement(anchor, windowSize, popupContentSize, with(density) { 18.dp.roundToPx() }, with(density) { 72.dp.roundToPx() })
                    .also { placement = it }.offset
            }
        }
    }
    val shape = remember(placement, density) {
        GenericShape { size, _ ->
            val radius = with(density) { 16.dp.toPx() }
            val tipHeight = with(density) { 10.dp.toPx() }
            val halfTip = with(density) { 7.dp.toPx() }
            val x = placement.pointerX
            val top = if (placement.below) tipHeight else 0f
            val bottom = if (placement.below) size.height else size.height - tipHeight
            moveTo(radius, top)
            if (placement.below) { lineTo(x - halfTip, top); lineTo(x, 0f); lineTo(x + halfTip, top) }
            lineTo(size.width - radius, top); quadraticTo(size.width, top, size.width, top + radius)
            lineTo(size.width, bottom - radius); quadraticTo(size.width, bottom, size.width - radius, bottom)
            if (!placement.below) { lineTo(x + halfTip, bottom); lineTo(x, size.height); lineTo(x - halfTip, bottom) }
            lineTo(radius, bottom); quadraticTo(0f, bottom, 0f, bottom - radius)
            lineTo(0f, top + radius); quadraticTo(0f, top, radius, top); close()
        }
    }
    Popup(
        popupPositionProvider = provider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        Surface(
            modifier = Modifier.width(width).testTag("member_answer_tooltip"),
            color = KiriColor.Bg.Secondary,
            shape = shape,
        ) {
            Row(Modifier.padding(start = 21.dp, top = if (placement.below) 28.dp else 18.dp, end = 12.dp, bottom = if (placement.below) 24.dp else 34.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(title, color = KiriColor.Text.Secondary, style = KiriDetailTypography.Title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(answer, color = if (actionEnabled) KiriColor.Text.Disabled else KiriColor.Text.Strong, style = com.kiri.uikit.KiriTypography.Value, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.width(1.dp).size(height = 50.dp, width = 1.dp).background(KiriColor.Border.Strong))
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onAction, enabled = actionEnabled, modifier = Modifier.size(30.dp)) {
                        Image(
                            painter = painterResource(R.drawable.kiri_poke),
                            contentDescription = "콕 찌르기",
                            modifier = Modifier.size(24.dp),
                            alpha = if (actionEnabled) 1f else 0.45f,
                        )
                    }
                    Text("콕 찌르기", color = if (actionEnabled) KiriColor.Text.Accent else KiriColor.Text.Disabled, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun KiriPokeNotice(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = KiriColor.Bg.Tertiary,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.padding(start = 28.dp, top = 18.dp, end = 28.dp, bottom = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Image(painterResource(R.drawable.kiri_poke_notice), "콕 찌르기", Modifier.size(24.dp))
            Text(message, color = KiriColor.Text.Strong, fontSize = 18.sp)
        }
    }
}

@Composable
fun KiriMissionCard(
    title: String?,
    place: String?,
    keywords: List<String>,
    completed: Boolean,
    unavailable: Boolean,
    onPlacesClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFlip: () -> Unit = {},
    flipIconModifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 132.dp)
            .background(KiriColor.Bg.Primary, RoundedCornerShape(8.dp)),
    ) {
        when {
            unavailable || title == null -> Column(
                Modifier.fillMaxSize().padding(horizontal = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("공개된 미션을 둘러보세요 →", color = KiriColor.Text.Strong, fontSize = 14.sp)
                Button(
                    onClick = onPlacesClick,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = KiriColor.Bg.Tertiary,
                        contentColor = KiriColor.Text.Secondary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("장소 둘러보기") }
            }
            completed -> Column(
                Modifier.fillMaxSize().padding(horizontal = 15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("이미 미션을 완료하셨군요!", color = KiriColor.Text.Accent, fontSize = 14.sp)
                Text(title, color = KiriColor.Text.Strong, fontSize = 18.sp)
                Button(
                    onClick = onPlacesClick,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = KiriColor.Bg.Tertiary,
                        contentColor = KiriColor.Text.Secondary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("오늘의 추억 기록하러가기") }
            }
            else -> Row(
                Modifier.fillMaxSize().padding(horizontal = 15.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f).height(96.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(title, color = KiriColor.Text.Strong, style = KiriDetailTypography.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(place.orEmpty(), color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        keywords.take(2).forEach { label ->
                            Surface(color = KiriColor.Bg.Elevated, shape = RoundedCornerShape(5.dp)) {
                                Text(label, Modifier.padding(horizontal = 8.dp, vertical = 6.dp), color = KiriColor.Text.Tertiary, style = KiriDetailTypography.Small.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = (-0.48).sp), maxLines = 1)
                            }
                        }
                    }
                }
                Spacer(Modifier.width(24.dp))
            }
        }
        KiriCardFlipButton("질문 카드로 전환", onFlip, Modifier.align(Alignment.TopEnd).padding(end = 15.dp, top = 18.dp), flipIconModifier)
    }
}

@Composable
fun KiriPlaceCard(
    title: String,
    place: String,
    description: String,
    rating: String?,
    imageRes: Int = R.drawable.kiri_place_card_image,
    modifier: Modifier = Modifier,
    detailStyle: Boolean = false,
) {
    Card(modifier.width(251.dp).height(284.dp), colors = CardDefaults.cardColors(KiriColor.Bg.Primary), shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 16.dp, bottomEnd = 16.dp)) {
        Column {
            Image(
                painter = painterResource(imageRes),
                contentDescription = title,
                modifier = Modifier.fillMaxWidth().height(144.dp).alpha(0.8f),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
            Column(
                Modifier.padding(start = 19.dp, end = 18.dp, top = 21.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(31.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(if (detailStyle) Modifier.fillMaxWidth() else Modifier.width(138.dp), verticalArrangement = Arrangement.spacedBy(if (detailStyle) 3.dp else 2.dp)) {
                        Text(title, color = KiriColor.Text.Strong, style = KiriDetailTypography.Title.copy(lineHeight = if (detailStyle) 22.sp else 27.sp), maxLines = 1, softWrap = false, overflow = if (detailStyle) TextOverflow.Ellipsis else TextOverflow.Visible)
                        Text(place, color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small.copy(fontWeight = FontWeight.Medium, lineHeight = if (detailStyle) 17.sp else 20.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    rating?.let {
                        Surface(color = KiriColor.Bg.Tertiary, shape = RoundedCornerShape(100.dp)) {
                            Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(R.drawable.kiri_star_small), null, Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp)); Text(it, color = KiriColor.Text.Tertiary, style = KiriDetailTypography.Small.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = (-0.48).sp))
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                    if (!detailStyle) Image(painterResource(R.drawable.kiri_place_comment), null, Modifier.size(width = 11.3965.dp, height = 10.4468.dp))
                    Text("“$description”", Modifier.weight(1f), color = KiriColor.Text.Tertiary, fontSize = 12.sp, lineHeight = if (detailStyle) 16.8.sp else 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, fontFamily = KiriPretendard, letterSpacing = (-0.48).sp)
                }
            }
        }
    }
}

@Composable
fun KiriInviteCode(
    code: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxWidth(), color = KiriColor.Bg.Elevated, shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("링크코드", color = KiriColor.Text.Tertiary, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(code, color = KiriColor.Text.Strong, fontSize = 20.sp, letterSpacing = 2.sp)
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) { Image(painterResource(R.drawable.kiri_copy), "복사", Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
fun KiriAnswerPopup(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().background(KiriColor.Bg.Primary, RoundedCornerShape(30.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("답변이 완료됐어요!", color = KiriColor.Text.Strong, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(4.dp))
            Text("다른 가족의 답변을 기다려볼까요?", color = KiriColor.Text.Secondary, fontSize = 16.sp)
            Spacer(Modifier.size(20.dp))
            Image(painterResource(R.drawable.kiri_answer_success), null, Modifier.size(131.dp))
        }
    }
}

@Composable
fun KiriContentCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Card(modifier, colors = CardDefaults.cardColors(KiriColor.Bg.Primary), shape = RoundedCornerShape(8.dp)) { Box(content = content) }
}

@Composable
fun KiriCardFlipButton(description: String, onFlip: () -> Unit, modifier: Modifier = Modifier, iconModifier: Modifier = Modifier) {
    Box(modifier.size(24.dp).testTag("home_card_flip").clickable(role = Role.Button, onClick = onFlip), contentAlignment = Alignment.TopEnd) {
        Image(painterResource(R.drawable.kiri_card_flip), description, Modifier.size(18.dp).then(iconModifier))
    }
}
