package com.kiri.feature.home.presentation

import com.kiri.feature.home.HomeViewModel
import com.kiri.feature.home.model.*

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.domain.home.*
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.color.KiriComponentColor
import com.kiri.uikit.component.KiriAvatarItem
import com.kiri.uikit.component.KiriContentCard
import com.kiri.uikit.component.KiriCardFlipButton
import com.kiri.uikit.component.KiriMemberAvatarGroup
import com.kiri.uikit.component.KiriMissionCard
import com.kiri.uikit.component.KiriPlaceCard
import com.kiri.uikit.component.KiriPokeNotice
import com.kiri.uikit.component.KiriTooltip
import com.kiri.uikit.component.KiriAppBar
import com.kiri.uikit.R as UiR
import androidx.compose.ui.res.painterResource
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt


@Composable
fun HomeRoute(
    onDestination: (HomeUiDestination) -> Unit = {},
    onInviteFamily: (String) -> Unit = {},
    onMissionClick: (String) -> Unit = {},
    onScheduleClick: (String) -> Unit = { onDestination(HomeUiDestination.Schedule) },
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentDestination by rememberUpdatedState(onDestination)
    val currentInvite by rememberUpdatedState(onInviteFamily)
    val currentMission by rememberUpdatedState(onMissionClick)
    val currentSchedule by rememberUpdatedState(onScheduleClick)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is HomeUiEffect.Navigate -> currentDestination(effect.destination)
                    is HomeUiEffect.InviteFamily -> currentInvite(effect.code)
                    is HomeUiEffect.OpenMission -> currentMission(effect.id)
                    is HomeUiEffect.OpenSchedule -> currentSchedule(effect.id)
                }
            }
        }
    }
    HomeScreen(state, viewModel::onEvent)
}

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
    fun anchor(target: HomeTutorialTarget) = Modifier.testTag("home_anchor_${target.name}").onGloballyPositioned { tutorialAnchors[target] = it.boundsInWindow() }
    LaunchedEffect(state.tutorialStep, home != null, tutorialAnchors.containsKey(HomeTutorialTarget.CARD), tutorialAnchors.containsKey(HomeTutorialTarget.RECOMMENDATIONS)) {
        if (state.tutorialStep != null && home != null) {
            withFrameNanos { }
            val extra = with(density) { 160.dp.toPx() }
            val above = with(density) { 80.dp.toPx() }
            val card = tutorialAnchors[HomeTutorialTarget.CARD]
            val title = tutorialAnchors[HomeTutorialTarget.RECOMMENDATIONS]
            when (state.tutorialStep) {
                HomeTutorialStep.INITIAL -> {
                    val start = tutorialAnchors[HomeTutorialTarget.ANSWERS]?.top ?: 0f
                    initialTarget.bringIntoView(card?.let { Rect(0f, 0f, it.width, it.bottom - start + extra) })
                }
                HomeTutorialStep.MISSION -> cardTarget.bringIntoView(card?.let { Rect(0f, -above, it.width, it.height + extra) })
                HomeTutorialStep.RECOMMENDATIONS -> recommendationsTarget.bringIntoView(title?.let { Rect(0f, 0f, it.width, it.height + extra) })
                null -> return@LaunchedEffect
            }
        }
    }
    androidx.activity.compose.BackHandler(enabled = state.tutorialStep != null) { onEvent(HomeUiEvent.TutorialDismissed) }
    var pullDistance by remember { mutableFloatStateOf(0f) }
    val refreshConnection = remember(state.loading, state.busy) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) pullDistance += available.y
                return Offset.Zero
            }
            override suspend fun onPostFling(consumed: androidx.compose.ui.unit.Velocity, available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (pullDistance > 110f && !state.loading && !state.busy) onEvent(HomeUiEvent.RetryClicked)
                pullDistance = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }
    Box(modifier.fillMaxSize().testTag("home_screen").onGloballyPositioned { homeOrigin = it.boundsInWindow().topLeft }.background(HomeBackground).nestedScroll(refreshConnection)) {
        if (state.editing) {
            home?.question?.let { q -> AnswerEditor(q.text, state.draft, state.busy, state.error, { onEvent(HomeUiEvent.AnswerChanged(it)) }, { onEvent(HomeUiEvent.AnswerSaveClicked) }, { onEvent(HomeUiEvent.BackClicked) }) }
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(scrollState)) {
                TopBar(home?.familyName ?: "둘러보기") { onDestination(HomeUiDestination.Notifications) }; Spacer(Modifier.height(11.dp))
                Column(Modifier.padding(horizontal = 18.dp)) {
                if (home != null && !home.browsing && !state.loading && !state.loadError) {
                    HomeTasteBanner(home.members.firstOrNull { it.id == home.currentUserId }?.nickname ?: "우리 가족")
                    Spacer(Modifier.height(48.dp))
                }
                when { state.loading -> LoadingHome(); state.loadError || home == null -> ErrorHome { onEvent(HomeUiEvent.RetryClicked) }; home.browsing -> BrowseCard(onDestination); else -> {
                    selectHomeSchedule(home.schedules, Instant.now(), ZoneId.of("Asia/Seoul"))?.let { ScheduleCard(it) { onScheduleClick(it.schedule.id) } }
                    Spacer(Modifier.height(18.dp))
                    Column(Modifier.bringIntoViewRequester(initialTarget)) {
                        AnswersHeader(anchor(HomeTutorialTarget.ANSWERS), anchor(HomeTutorialTarget.INVITE)) { onEvent(HomeUiEvent.InviteFamilyClicked) }
                        StoryRow(home, { onEvent(HomeUiEvent.MemberSelected(it)) }, memberBounds)
                        Spacer(Modifier.height(32.dp))
                        QuestionMissionSection(home, state.showingMission, onDestination, { onEvent(HomeUiEvent.CardFlipped) }, { onEvent(HomeUiEvent.AnswerEditClicked) }, onMissionClick, Modifier.bringIntoViewRequester(cardTarget).then(anchor(HomeTutorialTarget.CARD)), anchor(HomeTutorialTarget.FLIP))
                    }
                    RecommendationSection(home.recommendations, Modifier.bringIntoViewRequester(recommendationsTarget).then(anchor(HomeTutorialTarget.RECOMMENDATIONS)))
                } }
                Spacer(Modifier.height(28.dp))
                }
            }
        }
        if (state.answerSaved) AnswerSavedDialog { onEvent(HomeUiEvent.NoticeDismissed) }
        home?.let { h -> state.selectedMemberId?.let { id ->
            Box(Modifier.fillMaxSize().zIndex(3f).background(KiriColor.State.Overlay).clickable { onEvent(HomeUiEvent.MemberDismissed) })
            memberBounds[id]?.let { bounds -> MemberAnswerToast(h, id, state.busy, bounds, { onEvent(HomeUiEvent.PokeClicked) }, { onEvent(HomeUiEvent.MemberDismissed) }) }
        } }
        state.notice?.let { message -> Box(Modifier.fillMaxSize().zIndex(4f).clickable { onEvent(HomeUiEvent.NoticeDismissed) }) { NoticeBar(message) } }
        state.tutorialStep?.let { step ->
            HomeTutorialOverlay(step, tutorialAnchors.mapValues { (_, rect) -> rect.translate(-homeOrigin.x, -homeOrigin.y) }, { onEvent(HomeUiEvent.TutorialNext) }, { onEvent(HomeUiEvent.TutorialDismissed) })
        }
    }
}

@Composable private fun TopBar(name: String, onNotifications: () -> Unit) {
    KiriAppBar(name, trailingIconSlot = {
        Box(Modifier.size(48.dp).clickable(onClick = onNotifications).testTag("open_notifications"), contentAlignment = Alignment.Center) {
            Image(painterResource(UiR.drawable.kiri_notifications), "알림", Modifier.size(width = 20.dp, height = 17.5.dp))
        }
    })
}
@Composable private fun HomeTasteBanner(name: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 84.dp).background(KiriColor.Bg.Elevated, RoundedCornerShape(16.dp)).padding(horizontal = 21.dp, vertical = 15.dp).testTag("home_taste_banner"), verticalAlignment = Alignment.CenterVertically) {
        Text("${name}님의\n취향을 더 알고싶어요!", Modifier.weight(1f), color = KiriColor.Text.Strong, style = com.kiri.uikit.component.KiriDetailTypography.Title)
        Text("취향테스트 참여하러가기", color = KiriColor.Text.Secondary, style = com.kiri.uikit.component.KiriDetailTypography.Small)
    }
}
@Composable private fun LoadingHome() { Column(Modifier.fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(14.dp)) { repeat(4) { Box(Modifier.fillMaxWidth().height(if (it==0) 48.dp else 110.dp).clip(RoundedCornerShape(12.dp)).background(HomeCard)) } } }
@Composable private fun ErrorHome(onRetry: () -> Unit) { Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment=Alignment.CenterHorizontally) { Text("홈을 불러오지 못했어요.", color=HomeStrong); Spacer(Modifier.height(14.dp)); Button(onClick=onRetry) { Text("다시 시도") } } }

@Composable private fun AnswersHeader(labelModifier: Modifier = Modifier, inviteModifier: Modifier = Modifier, onInvite: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("답변 모아보기", labelModifier, color = KiriColor.Text.Strong, style = com.kiri.uikit.component.KiriDetailTypography.Title)
        Button(onClick = onInvite, modifier = inviteModifier.heightIn(min = 30.dp), contentPadding = PaddingValues(horizontal = 12.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = KiriComponentColor.Button.FilledNeutral.BgDefault, contentColor = KiriComponentColor.Button.FilledNeutral.ContentDefault)) {
            Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(2.dp)); Text("가족 초대하기", fontSize = 12.sp)
        }
    }
}
@Composable private fun BrowseCard(onDestination: (HomeUiDestination) -> Unit) { Card(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(HomeCard), shape=RoundedCornerShape(16.dp)) { Column(Modifier.padding(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) { Text("가족과 함께하는 질문을 시작해보세요", color=HomeStrong, fontSize=18.sp); Text("가족공간을 만들거나 초대코드로 참여하면 오늘의 질문을 받을 수 있어요.", color=HomeSecondary, fontSize=14.sp); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick={onDestination(HomeUiDestination.CreateFamily)}) { Text("가족 만들기") }; Button(onClick={onDestination(HomeUiDestination.JoinFamily)}) { Text("가족 참여하기") } } } } }

@Composable private fun StoryRow(home: FamilyHome, onMemberSelected: (String) -> Unit, bounds: MutableMap<String, Rect>) {
    val view = androidx.compose.ui.platform.LocalView.current
    KiriMemberAvatarGroup(
        members = home.members.map { member -> KiriAvatarItem(member.id, if (member.id == home.currentUserId) "내 답변" else member.nickname, member.id in home.answers, member.accessible, member.id == home.currentUserId, member.role) },
        onMemberClick = { onMemberSelected(it.id) },
        onPositioned = { id, coordinates ->
            val viewOrigin = IntArray(2).also(view::getLocationOnScreen)
            val rootPosition = coordinates.positionInRoot()
            bounds[id] = Rect(rootPosition.x + viewOrigin[0], rootPosition.y + viewOrigin[1], rootPosition.x + viewOrigin[0] + coordinates.size.width, rootPosition.y + viewOrigin[1] + coordinates.size.height)
        },
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    )
}

@Composable
private fun QuestionMissionSection(
    home: FamilyHome,
    showingMission: Boolean,
    onDestination: (HomeUiDestination) -> Unit,
    onFlip: () -> Unit,
    onEdit: () -> Unit,
    onMissionClick: (String) -> Unit,
    cardModifier: Modifier = Modifier,
    flipIconModifier: Modifier = Modifier,
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(if (showingMission) "이번주 미션이에요!" else "오늘 나의 질문카드", Modifier.weight(1f), color = KiriColor.Text.Strong, style = com.kiri.uikit.component.KiriDetailTypography.Title)
            Text("다른 가족들의 답변을 기다려요!", Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, color = KiriColor.Text.Secondary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        Box(cardModifier) { QuestionMissionCard(home, showingMission, onDestination, onFlip, onEdit, onMissionClick, flipIconModifier) }
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
) {
    val modifier = Modifier.fillMaxWidth()
        .clickable(enabled = if (showingMission) home.mission?.unavailable == false else home.question != null) {
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
                )
            } else QuestionCardContent(home, onFlip, flipIconModifier)
        }
    }
}

@Composable private fun QuestionCardContent(home: FamilyHome, onFlip: () -> Unit, flipIconModifier: Modifier) {
    val q = home.question
    val answer = home.answers[home.currentUserId]
    Column(Modifier.fillMaxWidth().heightIn(min = 136.dp).padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (answer == null) "오늘 질문에 답변해주세요!" else "답변완료", color = KiriColor.Text.Secondary, style = com.kiri.uikit.component.KiriDetailTypography.Body.copy(fontSize = 14.sp, lineHeight = 21.sp))
                Text(if (q == null) "오늘의 질문이 아직 배정되지 않았어요." else "Q1. ${q.text}", color = KiriColor.Text.Strong, style = com.kiri.uikit.component.KiriDetailTypography.Title)
            }
            KiriCardFlipButton("미션 카드로 전환", onFlip, iconModifier = flipIconModifier)
        }
        Text("A. ${answer ?: "답변미완료"}", Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End, color = if (answer == null) KiriColor.Text.Disabled else KiriColor.Text.Accent, style = com.kiri.uikit.component.KiriDetailTypography.Title)
    }
}


@Composable private fun RecommendationSection(items: List<HomeRecommendation>, titleModifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    Column(Modifier.padding(top = 48.dp)) {
        Text("엄마와 함께라면 여기!", titleModifier, color = HomeStrong, fontSize = 18.sp)
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items.forEach { recommendation ->
                KiriPlaceCard(
                    title = recommendation.title,
                    place = recommendation.place,
                    description = recommendation.description.ifBlank { recommendation.keywords.joinToString(", ") },
                    rating = recommendation.rating,
                )
            }
        }
    }
}

@Composable private fun ScheduleCard(card: HomeScheduleCard,onClick:()->Unit){val today=card.kind==HomeScheduleCard.Kind.TODAY;val label=when(card.kind){HomeScheduleCard.Kind.RESPONSE_REQUIRED->"응답이 필요한 일정";HomeScheduleCard.Kind.TODAY->"오늘 약속 당일이에요!";HomeScheduleCard.Kind.UPCOMING->"다가오는 일정"};Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(KiriColor.Bg.Secondary),shape=RoundedCornerShape(8.dp)){Row(Modifier.padding(horizontal=18.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(label,color=if(today)KiriColor.Text.Accent else KiriColor.Text.Strong,fontSize=16.sp);if(today)Text("코스를 확인하세요",color=KiriColor.Text.Strong,fontSize=16.sp) else Text(card.schedule.title,color=KiriColor.Text.Strong,fontSize=16.sp)};Icon(Icons.Default.ChevronRight,"일정 열기",tint=KiriColor.Icon.Secondary)}}}

@Composable private fun MemberAnswerToast(home: FamilyHome, memberId:String, busy:Boolean, bounds:Rect, onPoke:()->Unit, onDismiss:()->Unit){
    val m=home.members.firstOrNull{it.id==memberId}?:return; val can=home.canPoke(m.id)
    KiriTooltip("${m.nickname}의 가장 좋아하는 음식은",home.answers[m.id]?:"아직 답변하지 않았어요",can&&!busy,onPoke,IntRect(bounds.left.roundToInt(), bounds.top.roundToInt(), bounds.right.roundToInt(), bounds.bottom.roundToInt()), onDismiss)
}
@Composable private fun NoticeBar(message:String){KiriPokeNotice(message, Modifier.fillMaxWidth().padding(horizontal=18.dp, vertical=12.dp))}

private class PreviewHomeRepository(private val browsing:Boolean=false):HomeRepository{private val q=DailyQuestion("preview-question",LocalDate.now(),"내가 가장 좋아하는 음식은?");val home=FamilyHome(LocalDate.now(),"me",if(browsing)null else "끼리네 가족",if(browsing)emptyList() else listOf(FamilyMember("me","나","자녀"),FamilyMember("mother","마마","엄마"),FamilyMember("father","빠빠","아빠"),FamilyMember("sibling","돌돌이","자녀")),if(browsing)null else q,if(browsing)emptyMap() else mapOf("mother" to "뚝배기불고기"),emptySet(),if(browsing)null else WeeklyMission("preview-mission","광장시장에서 시장음식 먹어보기","광장시장 · 종로구",listOf("대중교통 편리","혼잡도 높음"),false,place="광장시장 · 종로구"),recommendations=if(browsing)emptyList() else listOf(HomeRecommendation("preview-place","경복궁에서 사진찍기","경복궁 종로구",listOf("대중교통 편리","가족과 함께"),"4.6",description="대중교통이 편리하고, 부모님이랑 가기 좋아요! 추천합니다")));override suspend fun load()=home;override suspend fun saveAnswer(question:DailyQuestion,answer:String)=home;override suspend fun poke(question:DailyQuestion,memberId:String)=home}
@Preview(name="Home",showBackground=true,backgroundColor=0xFF202020,device="spec:width=411dp,height=891dp,dpi=420")
@Composable private fun HomePreview(){HomeScreen(HomeUiState(loading=false, home=PreviewHomeRepository().home), {})}
@Preview(name="Home browsing",showBackground=true,backgroundColor=0xFF202020,device="spec:width=411dp,height=891dp,dpi=420")
@Composable private fun HomeBrowsingPreview(){HomeScreen(HomeUiState(loading=false, home=PreviewHomeRepository(true).home), {})}

@Preview
@Composable
private fun HomeScreenPreview() { HomeScreen(HomeUiState(), {}) }
