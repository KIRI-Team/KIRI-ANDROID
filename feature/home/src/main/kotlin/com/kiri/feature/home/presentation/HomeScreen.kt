package com.kiri.feature.home.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.domain.home.*
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.color.KiriComponentColor
import com.kiri.uikit.component.KiriAvatarItem
import com.kiri.uikit.component.KiriContentCard
import com.kiri.uikit.component.KiriMemberAvatarGroup
import com.kiri.uikit.component.KiriMissionCard
import com.kiri.uikit.component.KiriPlaceCard
import com.kiri.uikit.component.KiriPokeNotice
import com.kiri.uikit.component.KiriTooltip
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

enum class HomeUiDestination { CreateFamily, JoinFamily, InviteFamily, Schedule, AnswerHistory, Mission, OpenPlaces }

@Composable
fun HomeScreen(onDetailClick: () -> Unit = {}, onDestination: (HomeUiDestination) -> Unit = {}, onInviteFamily: (String) -> Unit = {}, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle(); val home = state.home
    val offsets = remember { mutableStateMapOf<String, IntOffset>() }
    var pullDistance by remember { mutableFloatStateOf(0f) }
    val refreshConnection = remember(state.loading, state.busy) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) pullDistance += available.y
                return Offset.Zero
            }
            override suspend fun onPostFling(consumed: androidx.compose.ui.unit.Velocity, available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (pullDistance > 110f && !state.loading && !state.busy) viewModel.onAction(HomeAction.Retry)
                pullDistance = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }
    Box(Modifier.fillMaxSize().background(HomeBackground).nestedScroll(refreshConnection)) {
        if (state.editing) {
            home?.question?.let { q -> AnswerEditor(q.text, state.draft, state.busy, state.error, { viewModel.onAction(HomeAction.ChangeAnswer(it)) }, { viewModel.onAction(HomeAction.SaveAnswer) }, { viewModel.onAction(HomeAction.Back) }) }
        } else {
            Column(Modifier.fillMaxSize().testTag("home_screen").statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
                TopBar(home?.familyName ?: "둘러보기"); Spacer(Modifier.height(23.dp))
                when { state.loading -> LoadingHome(); state.loadError || home == null -> ErrorHome { viewModel.onAction(HomeAction.Retry) }; home.browsing -> BrowseCard(onDestination); else -> {
                    selectHomeSchedule(home.schedules, Instant.now(), ZoneId.of("Asia/Seoul"))?.let { ScheduleCard(it, onDestination) }
                    Spacer(Modifier.height(18.dp)); AnswersHeader { home.inviteCode?.let(onInviteFamily) }; StoryRow(home, viewModel::onAction, offsets); Spacer(Modifier.height(32.dp)); QuestionMissionSection(home, state.showingMission, onDestination, viewModel::onAction); RecommendationSection(home.recommendations)
                } }
                Spacer(Modifier.height(28.dp))
            }
        }
        if (state.answerSaved) AnswerSavedDialog { viewModel.onAction(HomeAction.DismissNotice) }
        home?.let { h -> state.selectedMemberId?.let { id ->
            Box(Modifier.fillMaxSize().zIndex(3f).background(KiriColor.State.Overlay).clickable { viewModel.onAction(HomeAction.DismissMember) })
            MemberAnswerToast(h, id, state.busy, offsets[id], viewModel::onAction)
        } }
        state.notice?.let { message -> Box(Modifier.fillMaxSize().zIndex(4f).clickable { viewModel.onAction(HomeAction.DismissNotice) }) { NoticeBar(message) } }
    }
}

@Composable private fun TopBar(name: String) { Box(Modifier.fillMaxWidth().height(52.dp)) { Text(name, Modifier.align(Alignment.Center), color=HomeStrong, fontSize=18.sp, fontWeight=FontWeight.SemiBold); Icon(Icons.Default.NotificationsNone, "알림", Modifier.align(Alignment.CenterEnd).size(22.dp), tint=HomeSecondary) } }
@Composable private fun LoadingHome() { Column(Modifier.fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(14.dp)) { repeat(4) { Box(Modifier.fillMaxWidth().height(if (it==0) 48.dp else 110.dp).clip(RoundedCornerShape(12.dp)).background(HomeCard)) } } }
@Composable private fun ErrorHome(onRetry: () -> Unit) { Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment=Alignment.CenterHorizontally) { Text("홈을 불러오지 못했어요.", color=HomeStrong); Spacer(Modifier.height(14.dp)); Button(onClick=onRetry) { Text("다시 시도") } } }

@Composable private fun AnswersHeader(onInvite: () -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) { Text("답변 모아보기", Modifier.weight(1f), color=KiriColor.Text.Strong, fontSize=18.sp); Button(onClick=onInvite, modifier=Modifier.height(30.dp), contentPadding=PaddingValues(horizontal=12.dp), shape=RoundedCornerShape(8.dp), colors=ButtonDefaults.buttonColors(containerColor=KiriComponentColor.Button.FilledNeutral.BgDefault, contentColor=KiriComponentColor.Button.FilledNeutral.ContentDefault)) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(2.dp)); Text("가족 초대하기", fontSize=12.sp) } } }
@Composable private fun BrowseCard(onDestination: (HomeUiDestination) -> Unit) { Card(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(HomeCard), shape=RoundedCornerShape(16.dp)) { Column(Modifier.padding(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) { Text("가족과 함께하는 질문을 시작해보세요", color=HomeStrong, fontSize=18.sp); Text("가족공간을 만들거나 초대코드로 참여하면 오늘의 질문을 받을 수 있어요.", color=HomeSecondary, fontSize=14.sp); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick={onDestination(HomeUiDestination.CreateFamily)}) { Text("가족 만들기") }; Button(onClick={onDestination(HomeUiDestination.JoinFamily)}) { Text("가족 참여하기") } } } } }

@Composable private fun StoryRow(home: FamilyHome, onAction: (HomeAction) -> Unit, offsets: MutableMap<String, IntOffset>) { KiriMemberAvatarGroup(members=home.members.map { member -> KiriAvatarItem(member.id, if (member.id==home.currentUserId) "내 답변" else member.nickname, member.id in home.answers, member.accessible, member.id==home.currentUserId, member.role) }, onMemberClick={member -> onAction(HomeAction.SelectMember(member.id))}, onPositioned={id, coordinates -> val p=coordinates.positionInRoot(); offsets[id]=IntOffset((p.x + coordinates.size.width / 2f - 103f).roundToInt(), (p.y + coordinates.size.height).roundToInt() + 20)}, modifier=Modifier.fillMaxWidth().padding(top=16.dp)) }

@Composable private fun QuestionMissionSection(home: FamilyHome, showingMission: Boolean, onDestination: (HomeUiDestination) -> Unit, onAction: (HomeAction) -> Unit) { Column { Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.Bottom) { Text(if (showingMission) "이번주 미션이에요!" else "오늘 나의 질문카드", Modifier.weight(1f), color=KiriColor.Text.Strong, fontSize=18.sp); Text("다른 가족들의 답변을 기다려요!", color=KiriColor.Text.Secondary, fontSize=11.sp) }; Spacer(Modifier.height(12.dp)); QuestionMissionCard(home, showingMission, onDestination, onAction) } }
@Composable private fun QuestionMissionCard(home: FamilyHome, showingMission: Boolean, onDestination: (HomeUiDestination) -> Unit, onAction: (HomeAction) -> Unit) { var drag by remember(showingMission) { mutableFloatStateOf(0f) }; val modifier=Modifier.fillMaxWidth().pointerInput(showingMission) { detectHorizontalDragGestures(onHorizontalDrag={c,a->c.consume();drag+=a}, onDragEnd={if(abs(drag)>70)onAction(HomeAction.Flip);drag=0f}, onDragCancel={drag=0f}) }.clickable(enabled=!showingMission&&home.question!=null){onAction(HomeAction.EditAnswer)}.testTag(if(showingMission) "home_mission_card" else "home_question_card"); KiriContentCard(modifier=modifier){AnimatedContent(showingMission,label="question-mission-swipe"){if(it) home.mission.let { mission -> KiriMissionCard(mission?.title, mission?.place ?: mission?.description, mission?.keywords.orEmpty(), mission?.completed == true, mission?.unavailable == true, { onDestination(HomeUiDestination.OpenPlaces) }) } else QuestionCardContent(home)}} }
@Composable private fun QuestionCardContent(home: FamilyHome) { val q=home.question; val answer=home.answers[home.currentUserId]; Column(Modifier.fillMaxWidth().heightIn(min=136.dp).padding(horizontal=20.dp,vertical=25.dp), verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(answer==null)"오늘 질문에 답변해주세요!" else "답변완료",color=KiriColor.Text.Secondary,fontSize=14.sp); if(q==null)Text("오늘의 질문이 아직 배정되지 않았어요.",color=KiriColor.Text.Secondary) else {Text("Q1. ${q.text}",color=KiriColor.Text.Strong,fontSize=18.sp); Box(Modifier.fillMaxWidth(), contentAlignment=Alignment.CenterEnd){Text("A. ${answer ?: "답변미완료"}",color=answer?.let{KiriColor.Text.Accent}?:KiriColor.Text.Disabled,fontSize=18.sp,fontWeight=FontWeight.Medium)}}} }


@Composable private fun RecommendationSection(items: List<HomeRecommendation>) {
    if (items.isEmpty()) return
    Column(Modifier.padding(top = 48.dp)) {
        Text("엄마와 함께라면 여기!", color = HomeStrong, fontSize = 18.sp)
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

@Composable private fun ScheduleCard(card: HomeScheduleCard,onDestination:(HomeUiDestination)->Unit){val today=card.kind==HomeScheduleCard.Kind.TODAY;val label=when(card.kind){HomeScheduleCard.Kind.RESPONSE_REQUIRED->"응답이 필요한 일정";HomeScheduleCard.Kind.TODAY->"오늘 약속 당일이에요!";HomeScheduleCard.Kind.UPCOMING->"다가오는 일정"};Card(onClick={onDestination(HomeUiDestination.Schedule)},modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(KiriColor.Bg.Secondary),shape=RoundedCornerShape(8.dp)){Row(Modifier.padding(horizontal=18.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(label,color=if(today)KiriColor.Text.Accent else KiriColor.Text.Strong,fontSize=16.sp);if(today)Text("코스를 확인하세요",color=KiriColor.Text.Strong,fontSize=16.sp) else Text(card.schedule.title,color=KiriColor.Text.Strong,fontSize=16.sp)};Icon(Icons.Default.ChevronRight,"일정 열기",tint=KiriColor.Icon.Secondary)}}}

@Composable private fun MemberAnswerToast(home: FamilyHome, memberId:String, busy:Boolean, offset:IntOffset?, onAction:(HomeAction)->Unit){
    val m=home.members.firstOrNull{it.id==memberId}?:return; val can=home.canPoke(m.id)
    KiriTooltip("${m.nickname}의 가장 좋아하는 음식은",home.answers[m.id]?:"아직 답변하지 않았어요",can&&!busy,{onAction(HomeAction.Poke)},offset?:IntOffset(18,260))
}
@Composable private fun NoticeBar(message:String){KiriPokeNotice(message, Modifier.fillMaxWidth().padding(horizontal=18.dp, vertical=12.dp))}

private class PreviewHomeRepository(private val browsing:Boolean=false):HomeRepository{private val q=DailyQuestion("preview-question",LocalDate.now(),"내가 가장 좋아하는 음식은?");private var h=FamilyHome(LocalDate.now(),"me",if(browsing)null else "끼리네 가족",if(browsing)emptyList() else listOf(FamilyMember("me","나","자녀"),FamilyMember("mother","마마","엄마"),FamilyMember("father","빠빠","아빠"),FamilyMember("sibling","돌돌이","자녀")),if(browsing)null else q,if(browsing)emptyMap() else mapOf("mother" to "뚝배기불고기"),emptySet(),if(browsing)null else WeeklyMission("preview-mission","광장시장에서 시장음식 먹어보기","광장시장 · 종로구",listOf("대중교통 편리","혼잡도 높음"),false,place="광장시장 · 종로구"),recommendations=if(browsing)emptyList() else listOf(HomeRecommendation("preview-place","경복궁에서 사진찍기","경복궁 종로구",listOf("대중교통 편리","가족과 함께"),"4.6",description="대중교통이 편리하고, 부모님이랑 가기 좋아요! 추천합니다")));override suspend fun load()=h;override suspend fun saveAnswer(question:DailyQuestion,answer:String):FamilyHome{h=h.copy(answers=h.answers+(h.currentUserId to answer));return h};override suspend fun poke(question:DailyQuestion,memberId:String):FamilyHome{h=h.copy(pokedMemberIds=h.pokedMemberIds+memberId);return h}}
@Preview(name="Home",showBackground=true,backgroundColor=0xFF202020,device="spec:width=411dp,height=891dp,dpi=420")
@Composable private fun HomePreview(){HomeScreen(viewModel=remember{HomeViewModel(PreviewHomeRepository())})}
@Preview(name="Home browsing",showBackground=true,backgroundColor=0xFF202020,device="spec:width=411dp,height=891dp,dpi=420")
@Composable private fun HomeBrowsingPreview(){HomeScreen(viewModel=remember{HomeViewModel(PreviewHomeRepository(true))})}



