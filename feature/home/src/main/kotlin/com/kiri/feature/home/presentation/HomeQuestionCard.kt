package com.kiri.feature.home.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.kiri.domain.home.*
import com.kiri.feature.home.model.*
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriCardFlipButton
import com.kiri.uikit.component.KiriContentCard
import com.kiri.uikit.component.KiriMissionCard

@Composable
internal fun QuestionMissionSection(
    home: FamilyHome,
    showingMission: Boolean,
    onDestination: (HomeUiDestination) -> Unit,
    onFlip: () -> Unit,
    onEdit: () -> Unit,
    onMissionClick: (String) -> Unit,
    cardModifier: Modifier = Modifier,
    flipIconModifier: Modifier = Modifier,
    onWeeklyScheduleClick: () -> Unit = {},
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                if (showingMission) "이번주 미션이에요!" else "오늘 나의 질문카드",
                Modifier.weight(1f),
                color = KiriColor.Text.Strong,
                style = com.kiri.uikit.component.KiriDetailTypography.Title,
            )
            Text(
                "다른 가족들의 답변을 기다려요!",
                Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                color = KiriColor.Text.Secondary,
                style = com.kiri.uikit.KiriTypography.BodyMedium,
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(cardModifier) {
            QuestionMissionCard(
                home,
                showingMission,
                onDestination,
                onFlip,
                onEdit,
                onMissionClick,
                flipIconModifier,
                onWeeklyScheduleClick,
            )
        }
    }
}

@Composable
private fun QuestionMissionCard(
    home: FamilyHome,
    showingMission: Boolean,
    onDestination: (HomeUiDestination) -> Unit,
    onFlip: () -> Unit,
    onEdit: () -> Unit,
    onMissionClick: (String) -> Unit,
    flipIconModifier: Modifier = Modifier,
    onWeeklyScheduleClick: () -> Unit = {},
) {
    val modifier =
        Modifier.fillMaxWidth()
            // 수평 드래그를 카드 클릭으로 처리해 답변 화면이 열리지 않도록 한다.
            // 수직 드래그는 바깥 스크롤이 계속 처리한다.
            .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> change.consume() } }
            .clickable(
                enabled =
                    if (showingMission) home.mission?.unavailable == false
                    else home.question != null
            ) {
                if (showingMission) home.mission?.id?.let(onMissionClick) else onEdit()
            }
            .testTag(if (showingMission) "home_mission_card" else "home_question_card")
    KiriContentCard(modifier = modifier) {
        AnimatedContent(showingMission, label = "question-mission-flip") { missionSide ->
            if (missionSide) {
                val mission = home.mission
                KiriMissionCard(
                    title = mission?.title,
                    place = mission?.place ?: mission?.description,
                    keywords = mission?.let { it.keywords.ifEmpty { it.tags } }.orEmpty(),
                    completed = mission?.completed == true,
                    unavailable = mission?.unavailable == true,
                    onPlacesClick = {
                        if (mission != null && !mission.unavailable) onMissionClick(mission.id)
                        else onDestination(HomeUiDestination.OpenPlaces)
                    },
                    onFlip = { onFlip() },
                    flipIconModifier = flipIconModifier,
                    onScheduleClick = if (home.browsing) null else onWeeklyScheduleClick,
                    scheduleLabel = if (home.schedules.any {
                        it.weeklyMissionId == mission?.id && it.status != ScheduleStatus.CANCELLED
                    }) "일정 보기" else "일정 잡기",
                )
            } else QuestionCardContent(home, onFlip, flipIconModifier)
        }
    }
}

@Composable
private fun QuestionCardContent(home: FamilyHome, onFlip: () -> Unit, flipIconModifier: Modifier) {
    val q = home.question
    val answer = home.answers[home.currentUserId]
    Column(
        Modifier.fillMaxWidth()
            .heightIn(min = 136.dp)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (answer == null) "오늘 질문에 답변해주세요!" else "답변완료",
                    color = KiriColor.Text.Secondary,
                    style =
                        com.kiri.uikit.component.KiriDetailTypography.Body.copy(
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                        ),
                )
                Text(
                    if (q == null) "오늘의 질문이 아직 배정되지 않았어요." else "Q1. ${q.text}",
                    color = KiriColor.Text.Strong,
                    style = com.kiri.uikit.component.KiriDetailTypography.Title,
                )
            }
            KiriCardFlipButton("미션 카드로 전환", onFlip, iconModifier = flipIconModifier)
        }
        Text(
            "A. ${answer ?: "답변미완료"}",
            Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            color = if (answer == null) KiriColor.Text.Disabled else KiriColor.Text.Accent,
            style = com.kiri.uikit.component.KiriDetailTypography.Title,
        )
    }
}
