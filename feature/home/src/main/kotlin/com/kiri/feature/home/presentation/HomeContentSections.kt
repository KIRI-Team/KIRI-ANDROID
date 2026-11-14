package com.kiri.feature.home.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.kiri.domain.home.*
import com.kiri.feature.home.model.*
import com.kiri.uikit.R as UiR
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriAppBar
import com.kiri.uikit.component.KiriAvatarItem
import com.kiri.uikit.component.KiriMemberAvatarGroup
import com.kiri.uikit.component.KiriPlaceCard
import com.kiri.uikit.component.KiriPokeNotice
import com.kiri.uikit.component.KiriTooltip
import kotlin.math.roundToInt

@Composable
internal fun TopBar(name: String, onNotifications: () -> Unit) {
    KiriAppBar(
        name,
        trailingIconSlot = {
            Box(
                Modifier.size(48.dp)
                    .clickable(onClick = onNotifications)
                    .testTag("open_notifications"),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(UiR.drawable.kiri_notifications),
                    "알림",
                    Modifier.size(width = 20.dp, height = 17.5.dp),
                )
            }
        },
    )
}

@Composable
internal fun HomeTasteBanner(name: String, onClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 84.dp)
            .background(KiriColor.Bg.Primary, RoundedCornerShape(16.dp))
            .padding(horizontal = 21.dp, vertical = 15.dp)
            .testTag("home_taste_banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${name}님의\n취향을 더 알고싶어요!",
            Modifier.weight(1f),
            color = KiriColor.Text.Strong,
            style = com.kiri.uikit.component.KiriDetailTypography.Title,
        )
        Text(
            "취향테스트 참여하러가기",
            modifier = Modifier
                .testTag("home_taste_action")
                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
                .padding(vertical = 10.dp),
            color = KiriColor.Text.Secondary,
            style = com.kiri.uikit.component.KiriDetailTypography.Small,
        )
    }
}

@Composable
internal fun LoadingHome() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(4) {
            Box(
                Modifier.fillMaxWidth()
                    .height(if (it == 0) 48.dp else 110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HomeCard)
            )
        }
    }
}

@Composable
internal fun ErrorHome(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = HomeStrong, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRetry) { Text("다시 시도") }
    }
}

@Composable
internal fun AnswersHeader(
    labelModifier: Modifier = Modifier,
    inviteModifier: Modifier = Modifier,
    onInvite: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "답변 모아보기",
            labelModifier,
            color = KiriColor.Text.Strong,
            style = com.kiri.uikit.component.KiriDetailTypography.Title,
        )
        com.kiri.uikit.component.KiriButton(
            onClick = onInvite,
            modifier = inviteModifier,
            text = "가족 초대하기",
            tone = com.kiri.uikit.component.KiriButtonTone.Neutral,
            layout = com.kiri.uikit.component.KiriButtonLayout.Inline,
            leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) },
        )
    }
}

@Composable
internal fun BrowseCard(onDestination: (HomeUiDestination) -> Unit) {
    Column(horizontalAlignment = Alignment.End) {
        Card(
            onClick = { onDestination(HomeUiDestination.CreateFamily) },
            modifier = Modifier.fillMaxWidth().testTag("home_create_family"),
            colors = CardDefaults.cardColors(KiriColor.Bg.Primary),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(48.dp).background(KiriColor.Bg.Secondary, CircleShape), contentAlignment = Alignment.Center) {
                    Image(painterResource(com.kiri.feature.home.R.drawable.home_family_email), null, Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("아직 가족 공간이 없군요!", color = KiriColor.Text.Tertiary, style = com.kiri.uikit.KiriTypography.BodySmall)
                    Text("가족공간 만들러 가기", color = KiriColor.Text.Strong, style = com.kiri.uikit.KiriTypography.TitleLarge)
                }
                Icon(Icons.Default.ChevronRight, null, tint = KiriColor.Text.Primary)
            }
        }
        TextButton(onClick = { onDestination(HomeUiDestination.JoinFamily) }, modifier = Modifier.testTag("home_join_family")) {
            Text("가족 참여하기", color = KiriColor.Text.Accent)
        }
    }
}

@Composable
internal fun StoryRow(
    home: FamilyHome,
    onMemberSelected: (String) -> Unit,
    bounds: MutableMap<String, Rect>,
) {
    val view = androidx.compose.ui.platform.LocalView.current
    KiriMemberAvatarGroup(
        members =
            home.members.map { member ->
                KiriAvatarItem(
                    member.id,
                    if (member.id == home.currentUserId) "내 답변" else member.nickname,
                    member.id in home.answers,
                    member.accessible,
                    member.id == home.currentUserId,
                    member.role,
                )
            },
        onMemberClick = { onMemberSelected(it.id) },
        onPositioned = { id, coordinates ->
            val viewOrigin = IntArray(2).also(view::getLocationOnScreen)
            val rootPosition = coordinates.positionInRoot()
            bounds[id] =
                Rect(
                    rootPosition.x + viewOrigin[0],
                    rootPosition.y + viewOrigin[1],
                    rootPosition.x + viewOrigin[0] + coordinates.size.width,
                    rootPosition.y + viewOrigin[1] + coordinates.size.height,
                )
        },
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    )
}

@Composable
internal fun RecommendationSection(
    items: List<HomeRecommendation>,
    titleModifier: Modifier = Modifier,
    onMissionClick: (String) -> Unit = {},
    loadFailed: Boolean = false,
    onRetry: () -> Unit = {},
) {
    Column(Modifier.padding(top = 48.dp)) {
        Text(
            "가족과 함께라면 여기!",
            titleModifier,
            color = HomeStrong,
            style = com.kiri.uikit.KiriTypography.TitleLarge,
        )
        if (items.isEmpty()) {
            Text(if (loadFailed) "추천을 불러오지 못했어요." else "아직 공개된 추천이 없어요.",
                Modifier.padding(top = 16.dp), color = HomeSecondary)
            if (loadFailed) TextButton(onClick = onRetry) { Text("다시 시도") }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items.forEach { recommendation ->
                KiriPlaceCard(
                    title = recommendation.title,
                    place = recommendation.place,
                    description =
                        recommendation.description.ifBlank {
                            recommendation.keywords.joinToString(", ")
                        },
                    rating = recommendation.rating,
                    modifier = Modifier.clickable { onMissionClick(recommendation.id) }
                        .testTag("home_recommendation_${recommendation.id}"),
                    image = { imageModifier ->
                        com.kiri.uikit.component.KiriAsyncImage(
                            recommendation.imageUrl, recommendation.title,
                            imageModifier.background(KiriColor.Bg.Tertiary),
                        )
                    },
                )
            }
        }
    }
}

@Composable
internal fun ScheduleCard(card: HomeScheduleCard, onClick: () -> Unit) {
    val today = card.kind == HomeScheduleCard.Kind.TODAY
    val label =
        when (card.kind) {
            HomeScheduleCard.Kind.RESPONSE_REQUIRED -> "응답이 필요한 일정"
            HomeScheduleCard.Kind.TODAY -> "오늘 약속 당일이에요!"
            HomeScheduleCard.Kind.UPCOMING -> "다가오는 일정"
        }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(if (today) KiriColor.Bg.Secondary else KiriColor.Bg.Primary),
        shape = RoundedCornerShape(if (today) 12.dp else 8.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                val title = androidx.compose.ui.text.buildAnnotatedString {
                    if (today) {
                        append("오늘 ")
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = KiriColor.Text.Accent))
                        append("약속 당일")
                        pop()
                        append("이에요!")
                    } else append(label)
                }
                Text(title, color = KiriColor.Text.Strong, style = com.kiri.uikit.KiriTypography.TitleMedium)
                if (today) Text("코스를 확인하세요", color = KiriColor.Text.Strong, fontSize = 16.sp)
                else Text(card.schedule.title, color = KiriColor.Text.Strong, fontSize = 16.sp)
            }
            Icon(Icons.Default.ChevronRight, "일정 열기", tint = KiriColor.Icon.Secondary)
        }
    }
}

@Composable
internal fun MemberAnswerToast(
    home: FamilyHome,
    memberId: String,
    busy: Boolean,
    bounds: Rect,
    onPoke: () -> Unit,
    onDismiss: () -> Unit,
) {
    val m = home.members.firstOrNull { it.id == memberId } ?: return
    val can = home.canPoke(m.id)
    val answer = home.answers[m.id]
    val question = home.question?.text?.trim().orEmpty()
    val nickname = m.nickname
    val title = when {
        answer != null -> "${nickname}의 답변"
        question.isEmpty() -> "${nickname}의 오늘 질문"
        question.startsWith("내가 ") -> "${nickname}의 ${question.removePrefix("내가 ")}"
        question.startsWith("나의 ") -> "${nickname}의 ${question.removePrefix("나의 ")}"
        else -> "$nickname · $question"
    }
    KiriTooltip(
        title,
        answer ?: "답변하지 않았어요",
        can && !busy,
        onPoke,
        IntRect(
            bounds.left.roundToInt(),
            bounds.top.roundToInt(),
            bounds.right.roundToInt(),
            bounds.bottom.roundToInt(),
        ),
        onDismiss,
        unanswered = answer == null,
    )
}

@Composable
internal fun NoticeBar(message: String) {
    KiriPokeNotice(message, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp))
}
