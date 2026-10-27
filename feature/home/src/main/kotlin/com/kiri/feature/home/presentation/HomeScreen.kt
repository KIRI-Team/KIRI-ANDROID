package com.kiri.feature.home.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.kiri.domain.home.*
import com.kiri.feature.home.model.*
import com.kiri.uikit.color.KiriColor
import java.time.ZoneId

@Composable
fun HomeScreen(state: HomeUiState, onEvent: (HomeUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val home = state.home
    val onDestination: (HomeUiDestination) -> Unit = { onEvent(HomeUiEvent.DestinationClicked(it)) }
    val onMissionClick: (String) -> Unit = { onEvent(HomeUiEvent.MissionClicked(it)) }
    val onScheduleClick: (String) -> Unit = { onEvent(HomeUiEvent.ScheduleClicked(it)) }
    val memberBounds = remember { mutableStateMapOf<String, Rect>() }
    val tutorialAnchors = remember { mutableStateMapOf<HomeTutorialTarget, Rect>() }
    var homeOrigin by remember { mutableStateOf(Offset.Zero) }
    val scrollState = rememberScrollState()
    val initialTarget = remember { BringIntoViewRequester() }
    val cardTarget = remember { BringIntoViewRequester() }
    val recommendationsTarget = remember { BringIntoViewRequester() }
    val density = androidx.compose.ui.platform.LocalDensity.current
    fun anchor(target: HomeTutorialTarget) =
        Modifier.testTag("home_anchor_${target.name}").onGloballyPositioned {
            tutorialAnchors[target] = it.boundsInWindow()
        }
    LaunchedEffect(
        state.tutorialStep,
        home != null,
        tutorialAnchors.containsKey(HomeTutorialTarget.CARD),
        tutorialAnchors.containsKey(HomeTutorialTarget.RECOMMENDATIONS),
    ) {
        if (state.tutorialStep != null && home != null) {
            withFrameNanos {}
            val extra = with(density) { 160.dp.toPx() }
            val above = with(density) { 80.dp.toPx() }
            val card = tutorialAnchors[HomeTutorialTarget.CARD]
            val title = tutorialAnchors[HomeTutorialTarget.RECOMMENDATIONS]
            when (state.tutorialStep) {
                HomeTutorialStep.INITIAL -> {
                    val start = tutorialAnchors[HomeTutorialTarget.ANSWERS]?.top ?: 0f
                    initialTarget.bringIntoView(
                        card?.let { Rect(0f, 0f, it.width, it.bottom - start + extra) }
                    )
                }
                HomeTutorialStep.MISSION ->
                    cardTarget.bringIntoView(
                        card?.let { Rect(0f, -above, it.width, it.height + extra) }
                    )
                HomeTutorialStep.RECOMMENDATIONS ->
                    recommendationsTarget.bringIntoView(
                        title?.let { Rect(0f, 0f, it.width, it.height + extra) }
                    )
                null -> return@LaunchedEffect
            }
        }
    }
    androidx.activity.compose.BackHandler(enabled = state.tutorialStep != null) {
        onEvent(HomeUiEvent.TutorialDismissed)
    }
    var pullDistance by remember { mutableFloatStateOf(0f) }
    val refreshConnection =
        remember(state.loading, state.busy) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y > 0f) pullDistance += available.y
                    return Offset.Zero
                }

                override suspend fun onPostFling(
                    consumed: androidx.compose.ui.unit.Velocity,
                    available: androidx.compose.ui.unit.Velocity,
                ): androidx.compose.ui.unit.Velocity {
                    if (pullDistance > 110f && !state.loading && !state.busy)
                        onEvent(HomeUiEvent.RetryClicked)
                    pullDistance = 0f
                    return androidx.compose.ui.unit.Velocity.Zero
                }
            }
        }
    Box(
        modifier
            .fillMaxSize()
            .testTag("home_screen")
            .onGloballyPositioned { homeOrigin = it.boundsInWindow().topLeft }
            .background(HomeBackground)
            .nestedScroll(refreshConnection)
    ) {
        if (state.editing) {
            home?.question?.let { q ->
                AnswerEditor(
                    q.text,
                    state.draft,
                    state.busy,
                    state.error,
                    { onEvent(HomeUiEvent.AnswerChanged(it)) },
                    { onEvent(HomeUiEvent.AnswerSaveClicked) },
                    { onEvent(HomeUiEvent.BackClicked) },
                )
            }
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(scrollState)) {
                TopBar(home?.familyName ?: "둘러보기") {
                    onDestination(HomeUiDestination.Notifications)
                }
                Spacer(Modifier.height(11.dp))
                Column(Modifier.padding(horizontal = 18.dp)) {
                    if (home != null && !home.browsing && !state.loading && !state.loadError) {
                        HomeTasteBanner(
                            home.members.firstOrNull { it.id == home.currentUserId }?.nickname
                                ?: "우리 가족"
                        )
                        Spacer(Modifier.height(48.dp))
                    }
                    when {
                        state.loading -> LoadingHome()
                        state.loadError || home == null ->
                            ErrorHome { onEvent(HomeUiEvent.RetryClicked) }
                        home.browsing -> BrowseCard(onDestination)
                        else -> {
                            val responseSchedules =
                                home.schedules.filter {
                                    it.status == ScheduleStatus.PENDING_RESPONSE ||
                                        it.status == ScheduleStatus.RECONFIRM_REQUIRED
                                }
                            selectHomeSchedule(
                                    responseSchedules,
                                    com.kiri.uikit.LocalKiriClock.current.instant(),
                                    ZoneId.of("Asia/Seoul"),
                                )
                                ?.let {
                                    ScheduleCard(it) { onScheduleClick(it.schedule.id) }
                                    Spacer(Modifier.height(18.dp))
                                }
                            selectHomeSchedule(
                                    home.schedules - responseSchedules.toSet(),
                                    com.kiri.uikit.LocalKiriClock.current.instant(),
                                    ZoneId.of("Asia/Seoul"),
                                )
                                ?.let {
                                    ScheduleCard(it) { onScheduleClick(it.schedule.id) }
                                    Spacer(Modifier.height(18.dp))
                                }
                            Column(Modifier.bringIntoViewRequester(initialTarget)) {
                                AnswersHeader(
                                    anchor(HomeTutorialTarget.ANSWERS),
                                    anchor(HomeTutorialTarget.INVITE),
                                ) {
                                    onEvent(HomeUiEvent.InviteFamilyClicked)
                                }
                                StoryRow(
                                    home,
                                    { onEvent(HomeUiEvent.MemberSelected(it)) },
                                    memberBounds,
                                )
                                Spacer(Modifier.height(32.dp))
                                QuestionMissionSection(
                                    home,
                                    state.showingMission,
                                    onDestination,
                                    { onEvent(HomeUiEvent.CardFlipped) },
                                    { onEvent(HomeUiEvent.AnswerEditClicked) },
                                    onMissionClick,
                                    Modifier.bringIntoViewRequester(cardTarget)
                                        .then(anchor(HomeTutorialTarget.CARD)),
                                    anchor(HomeTutorialTarget.FLIP),
                                )
                            }
                            RecommendationSection(
                                home.recommendations,
                                Modifier.bringIntoViewRequester(recommendationsTarget)
                                    .then(anchor(HomeTutorialTarget.RECOMMENDATIONS)),
                            )
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
        if (state.answerSaved) AnswerSavedDialog { onEvent(HomeUiEvent.NoticeDismissed) }
        home?.let { h ->
            state.selectedMemberId?.let { id ->
                Box(
                    Modifier.fillMaxSize()
                        .zIndex(3f)
                        .background(KiriColor.State.Overlay)
                        .clickable { onEvent(HomeUiEvent.MemberDismissed) }
                )
                memberBounds[id]?.let { bounds ->
                    MemberAnswerToast(
                        h,
                        id,
                        state.busy,
                        bounds,
                        { onEvent(HomeUiEvent.PokeClicked) },
                        { onEvent(HomeUiEvent.MemberDismissed) },
                    )
                }
            }
        }
        state.notice?.let { message ->
            Box(
                Modifier.fillMaxSize().zIndex(4f).clickable { onEvent(HomeUiEvent.NoticeDismissed) }
            ) {
                NoticeBar(message)
            }
        }
        state.tutorialStep?.let { step ->
            HomeTutorialOverlay(
                step,
                tutorialAnchors.mapValues { (_, rect) ->
                    rect.translate(-homeOrigin.x, -homeOrigin.y)
                },
                { onEvent(HomeUiEvent.TutorialNext) },
                { onEvent(HomeUiEvent.TutorialDismissed) },
            )
        }
    }
}
