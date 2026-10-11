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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        members.forEach { member ->
            Column(
                modifier = Modifier
                    .width(47.dp)
                    .clickable(enabled = member.enabled) { onMemberClick(member) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                KiriMemberAvatar(
                    answered = member.answered,
                    role = member.role,
                    showAddBadge = member.isCurrentUser,
                    modifier = Modifier.onGloballyPositioned { onPositioned(member.id, it) },
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = member.label,
                    color = KiriColor.Text.Primary,
                    style = KiriDetailTypography.Body.copy(fontWeight = FontWeight.Medium, lineHeight = if (member.isCurrentUser) 24.sp else 19.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun KiriMemberAvatar(answered: Boolean, role: String? = null, showAddBadge: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 47.dp, height = 45.dp)
            .semantics { stateDescription = if (answered) "답변 완료" else "미답변" },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(if (showAddBadge) R.drawable.kiri_my_answer else R.drawable.kiri_member_avatar),
            contentDescription = role,
            modifier = Modifier.size(width = if (showAddBadge) 47.dp else 45.dp, height = 45.dp),
        )
    }
}

@Composable
fun KiriTooltip(
    title: String,
    answer: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    offset: IntOffset,
    onDismiss: () -> Unit = {},
) {
    Popup(
        alignment = Alignment.TopStart,
        offset = offset,
        properties = PopupProperties(focusable = false),
    ) {
        Surface(
            modifier = Modifier.width(303.dp),
            color = KiriColor.Bg.Elevated,
            shape = KiriTooltipShape,
        ) {
            Row(Modifier.padding(start = 21.dp, top = 28.dp, end = 12.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(190.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(title, color = KiriColor.Text.Secondary, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(answer, color = if (actionEnabled) KiriColor.Text.Disabled else KiriColor.Text.Strong, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.width(1.dp).size(height = 50.dp, width = 1.dp).background(KiriColor.Border.Strong))
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onAction, enabled = actionEnabled, modifier = Modifier.size(30.dp)) {
                        Image(
                            painter = painterResource(R.drawable.kiri_poke),
                            contentDescription = "콕 찌르기",
                            modifier = Modifier.size(22.dp),
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
) {
    Box(
        modifier = modifier.fillMaxWidth().height(132.dp)
            .background(KiriColor.Bg.Secondary, RoundedCornerShape(8.dp)),
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
                Image(painterResource(R.drawable.kiri_mission_flip), "질문 카드로 전환", Modifier.size(18.dp).clickable(role = Role.Button, onClick = onFlip))
            }
        }
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
    Card(modifier.width(251.dp).height(284.dp), colors = CardDefaults.cardColors(if (detailStyle) KiriColor.Bg.Elevated else KiriColor.Bg.Secondary), shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 16.dp, bottomEnd = 16.dp)) {
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
    Card(modifier, colors = CardDefaults.cardColors(KiriColor.Bg.Secondary), shape = RoundedCornerShape(8.dp)) { Box(content = content) }
}

private val KiriTooltipShape = GenericShape { size, _ ->
    val radius = 16f; val x = size.width * .34f
    moveTo(x - 13f, 0f); lineTo(x, -20f); lineTo(x + 13f, 0f); lineTo(size.width - radius, 0f)
    quadraticTo(size.width, 0f, size.width, radius); lineTo(size.width, size.height - radius)
    quadraticTo(size.width, size.height, size.width - radius, size.height); lineTo(radius, size.height)
    quadraticTo(0f, size.height, 0f, size.height - radius); lineTo(0f, radius); quadraticTo(0f, 0f, radius, 0f); close()
}
