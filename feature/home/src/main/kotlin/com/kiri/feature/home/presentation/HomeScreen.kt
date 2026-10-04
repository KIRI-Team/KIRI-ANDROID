package com.kiri.feature.home.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.domain.home.*
import com.kiri.feature.home.R
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

enum class HomeUiDestination { CreateFamily, JoinFamily, InviteFamily, Schedule, AnswerHistory, Mission }

private val SpeechBubbleShape = GenericShape { size, _ ->
    val r = 16f; val x = size.width * .34f
    moveTo(x - 13f, 0f); lineTo(x, -20f); lineTo(x + 13f, 0f); lineTo(size.width-r, 0f)
    quadraticTo(size.width, 0f, size.width, r); lineTo(size.width, size.height-r); quadraticTo(size.width, size.height, size.width-r, size.height)
    lineTo(r, size.height); quadraticTo(0f, size.height, 0f, size.height-r); lineTo(0f, r); quadraticTo(0f, 0f, r, 0f); close()
}

@Composable
fun HomeScreen(onDetailClick: () -> Unit = {}, onDestination: (HomeUiDestination) -> Unit = {}, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle(); val home = state.home
    val offsets = remember { mutableStateMapOf<String, IntOffset>() }
    Box(Modifier.fillMaxSize().background(HomeBackground).pointerInput(state.loading) {
        var distance = 0f
        detectVerticalDragGestures(onVerticalDrag = { change, amount -> if (amount > 0) distance += amount; change.consume() }, onDragEnd = { if (distance > 110 && !state.loading && !state.busy) viewModel.onAction(HomeAction.Retry); distance = 0f }, onDragCancel = { distance = 0f })
    }) {
        if (state.editing) {
            home?.question?.let { q -> AnswerEditor(q.text, state.draft, state.busy, state.error, { viewModel.onAction(HomeAction.ChangeAnswer(it)) }, { viewModel.onAction(HomeAction.SaveAnswer) }, { viewModel.onAction(HomeAction.Back) }) }
        } else {
            Column(Modifier.fillMaxSize().testTag("home_screen").statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
                TopBar(home?.familyName ?: "둘러보기"); Spacer(Modifier.height(23.dp))
                when { state.loading -> LoadingHome(); state.loadError || home == null -> ErrorHome { viewModel.onAction(HomeAction.Retry) }; home.browsing -> BrowseCard(onDestination); else -> {
                    selectHomeSchedule(home.schedules, Instant.now(), ZoneId.of("Asia/Seoul"))?.let { ScheduleCard(it, onDestination) }
                    AnswersHeader { onDestination(HomeUiDestination.InviteFamily) }; StoryRow(home, viewModel::onAction, offsets); Spacer(Modifier.height(32.dp)); QuestionMissionSection(home, state.showingMission, viewModel::onAction); RecommendationSection(home.recommendations)
                } }
                Spacer(Modifier.height(28.dp))
            }
        }
        if (state.answerSaved) AnswerSavedDialog { viewModel.onAction(HomeAction.DismissNotice) }
        home?.let { h -> state.selectedMemberId?.let { MemberAnswerToast(h, it, state.busy, offsets[it], viewModel::onAction) } }
        state.notice?.let { message -> Box(Modifier.fillMaxSize().zIndex(4f).clickable { viewModel.onAction(HomeAction.DismissNotice) }) { NoticeBar(message) } }
    }
}

@Composable private fun TopBar(name: String) { Box(Modifier.fillMaxWidth().height(52.dp)) { Text(name, Modifier.align(Alignment.Center), color=HomeStrong, fontSize=18.sp, fontWeight=FontWeight.SemiBold); Icon(Icons.Default.NotificationsNone, "알림", Modifier.align(Alignment.CenterEnd).size(22.dp), tint=HomeSecondary) } }
@Composable private fun LoadingHome() { Column(Modifier.fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(14.dp)) { repeat(4) { Box(Modifier.fillMaxWidth().height(if (it==0) 48.dp else 110.dp).clip(RoundedCornerShape(12.dp)).background(HomeCard)) } } }
@Composable private fun ErrorHome(onRetry: () -> Unit) { Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment=Alignment.CenterHorizontally) { Text("홈을 불러오지 못했어요.", color=HomeStrong); Spacer(Modifier.height(14.dp)); Button(onClick=onRetry) { Text("다시 시도") } } }

@Composable private fun AnswersHeader(onInvite: () -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) { Text("답변 모아보기", Modifier.weight(1f), color=HomeStrong, fontSize=18.sp); Button(onClick=onInvite, modifier=Modifier.height(30.dp), contentPadding=PaddingValues(horizontal=12.dp), shape=RoundedCornerShape(8.dp), colors=ButtonDefaults.buttonColors(containerColor=HomeCard, contentColor=HomeSecondary)) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(2.dp)); Text("가족 초대하기", fontSize=12.sp) } } }
@Composable private fun BrowseCard(onDestination: (HomeUiDestination) -> Unit) { Card(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(HomeCard), shape=RoundedCornerShape(16.dp)) { Column(Modifier.padding(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) { Text("가족과 함께하는 질문을 시작해보세요", color=HomeStrong, fontSize=18.sp); Text("가족공간을 만들거나 초대코드로 참여하면 오늘의 질문을 받을 수 있어요.", color=HomeSecondary, fontSize=14.sp); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick={onDestination(HomeUiDestination.CreateFamily)}) { Text("가족 만들기") }; Button(onClick={onDestination(HomeUiDestination.JoinFamily)}) { Text("가족 참여하기") } } } } }

@Composable private fun StoryRow(home: FamilyHome, onAction: (HomeAction) -> Unit, offsets: MutableMap<String, IntOffset>) { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top=16.dp), horizontalArrangement=Arrangement.spacedBy(12.dp)) { home.members.forEach { member -> val answered = member.id in home.answers; Column(horizontalAlignment=Alignment.CenterHorizontally, modifier=Modifier.width(44.dp).clickable(enabled=member.accessible){onAction(HomeAction.SelectMember(member.id) )}) { Surface(shape=RoundedCornerShape(24.dp), border=BorderStroke(1.dp, if (answered) HomeAccent else Color(0xFF404040)), color=HomeCard, modifier=Modifier.size(45.dp).onGloballyPositioned { c -> val p=c.positionInRoot(); offsets[member.id]=IntOffset(p.x.roundToInt(), (p.y+c.size.height).roundToInt()+8) }) { Box { Image(painterResource(if (answered) R.drawable.home_avatar_active else R.drawable.home_avatar_inactive), member.nickname, Modifier.fillMaxSize()); if (member.id==home.currentUserId) Image(painterResource(R.drawable.home_add_badge), "가족 초대", Modifier.size(16.dp).align(Alignment.BottomEnd)) } }; Text(if (member.id==home.currentUserId) "내 답변" else member.nickname, color=HomeSecondary, fontSize=14.sp, maxLines=1, overflow=TextOverflow.Ellipsis) } } } }

@Composable private fun QuestionMissionSection(home: FamilyHome, showingMission: Boolean, onAction: (HomeAction) -> Unit) { Column { Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.Bottom) { Text(if (showingMission) "이번주 미션이에요!" else "오늘 나의 질문카드", Modifier.weight(1f), color=HomeStrong, fontSize=18.sp); Text("다른 가족들의 답변을 기다려요!", color=HomeSecondary, fontSize=11.sp) }; Spacer(Modifier.height(12.dp)); QuestionMissionCard(home, showingMission, onAction) } }
@Composable private fun QuestionMissionCard(home: FamilyHome, showingMission: Boolean, onAction: (HomeAction) -> Unit) { var drag by remember(showingMission) { mutableFloatStateOf(0f) }; val modifier=Modifier.fillMaxWidth().pointerInput(showingMission) { detectHorizontalDragGestures(onHorizontalDrag={c,a->c.consume();drag+=a}, onDragEnd={if(abs(drag)>70)onAction(HomeAction.Flip);drag=0f}, onDragCancel={drag=0f}) }.clickable(enabled=!showingMission&&home.question!=null){onAction(HomeAction.EditAnswer)}.testTag(if(showingMission) "home_mission_card" else "home_question_card"); Card(modifier, colors=CardDefaults.cardColors(HomeCard), shape=RoundedCornerShape(8.dp)){AnimatedContent(showingMission,label="question-mission-swipe"){if(it)MissionCardContent(home.mission)else QuestionCardContent(home)}} }
@Composable private fun QuestionCardContent(home: FamilyHome) { val q=home.question; Column(Modifier.padding(horizontal=20.dp,vertical=25.dp).heightIn(min=136.dp), verticalArrangement=Arrangement.spacedBy(8.dp)){Text("오늘 질문에 답변해주세요!",color=HomeSecondary,fontSize=14.sp); if(q==null)Text("오늘의 질문이 아직 배정되지 않았어요.",color=HomeSecondary) else {Text("Q1. ${q.text}",color=HomeStrong,fontSize=18.sp); Text("A. ${home.answers[home.currentUserId] ?: "답변미완료"}",color=home.answers[home.currentUserId]?.let{HomeAccent}?:Color(0xFF737373),fontSize=18.sp,fontWeight=FontWeight.Medium)}} }
@Composable private fun MissionCardContent(mission: WeeklyMission?) { if(mission==null){Text("이번 주 미션이 없어요.",Modifier.padding(20.dp),color=HomeSecondary);return}; Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.spacedBy(17.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(91.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF454545)));Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(mission.title,color=HomeStrong,fontSize=18.sp,fontWeight=FontWeight.SemiBold);Text(mission.place?:mission.description,color=HomeSecondary,fontSize=14.sp);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){mission.keywords.forEach{KeywordChip(it)}}}} }
@Composable private fun KeywordChip(label:String){Surface(color=Color(0xFF404040),shape=RoundedCornerShape(5.dp)){Text(label,Modifier.padding(horizontal=8.dp,vertical=6.dp),color=HomeSecondary,fontSize=12.sp)}}

@Composable private fun RecommendationSection(items: List<HomeRecommendation>){if(items.isEmpty())return;Column(Modifier.padding(top=48.dp)){Text("엄마와 함께라면 여기!",color=HomeStrong,fontSize=18.sp);Row(Modifier.horizontalScroll(rememberScrollState()).padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)){items.forEach{r->Card(Modifier.width(202.dp),colors=CardDefaults.cardColors(Color(0xFF404040)),shape=RoundedCornerShape(16.dp)){Column{Box(Modifier.fillMaxWidth().height(116.dp).background(Color(0xFF555555)));Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(r.title,Modifier.weight(1f),color=HomeStrong,fontSize=16.sp,maxLines=1,overflow=TextOverflow.Ellipsis);r.rating?.let{Surface(color=Color(0xFF4D4D4D),shape=RoundedCornerShape(14.dp)){Row(Modifier.padding(horizontal=8.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically){Text("★",color=HomeStrong,fontSize=12.sp);Spacer(Modifier.width(3.dp));Text(it,color=HomeSecondary,fontSize=12.sp)}}}};Text(r.place,color=HomeSecondary,fontSize=14.sp);Text("“${r.description.ifBlank{r.keywords.joinToString(", ")}}”",color=HomeSecondary,fontSize=12.sp,maxLines=3,overflow=TextOverflow.Ellipsis)}}}}}}}
@Composable private fun ScheduleCard(card: HomeScheduleCard,onDestination:(HomeUiDestination)->Unit){val label=when(card.kind){HomeScheduleCard.Kind.RESPONSE_REQUIRED->"응답이 필요한 일정";HomeScheduleCard.Kind.TODAY->"오늘의 일정";HomeScheduleCard.Kind.UPCOMING->"다가오는 일정"};Card(onClick={onDestination(HomeUiDestination.Schedule)},modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(HomeCard),shape=RoundedCornerShape(16.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(label,color=HomeAccent,fontSize=14.sp);Text(card.schedule.title,color=HomeStrong,fontSize=18.sp)};Icon(Icons.Default.ChevronRight,"일정 열기",tint=HomeSecondary)}}}

@Composable private fun MemberAnswerToast(home: FamilyHome, memberId:String, busy:Boolean, offset:IntOffset?, onAction:(HomeAction)->Unit){
    val m=home.members.firstOrNull{it.id==memberId}?:return; val can=home.canPoke(m.id)
    Dialog(onDismissRequest={onAction(HomeAction.DismissMember)}, properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.width(303.dp),color=Color(0xFF414141),shape=SpeechBubbleShape) {
            Row(Modifier.padding(start=21.dp,top=28.dp,end=12.dp,bottom=24.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.width(190.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text("${m.nickname}의 가장 좋아하는 음식은",color=HomeSecondary,fontSize=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text(home.answers[m.id]?:"아직 답변하지 않았어요",color=if(can)Color(0xFF737373)else HomeStrong,fontSize=20.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(12.dp)); Box(Modifier.width(1.dp).height(50.dp).background(Color(0xFF666666))); Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    IconButton(onClick={onAction(HomeAction.Poke)},enabled=can&&!busy,modifier=Modifier.size(30.dp)){Icon(Icons.Default.TouchApp,"콕 찌르기",tint=if(can)HomeAccent else Color(0xFF8C8C8C))}
                    Text("콕 찌르기",color=if(can)HomeAccent else Color(0xFF8C8C8C),fontSize=12.sp)
                }
            }
        }
    }
}
@Composable private fun NoticeBar(message:String){Surface(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=12.dp),color=Color(0xFF414141),shape=RoundedCornerShape(10.dp)){Row(Modifier.padding(horizontal=16.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.TouchApp,null,tint=HomeSecondary,modifier=Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text(message,color=HomeStrong,fontSize=14.sp)}}}

private class PreviewHomeRepository(private val browsing:Boolean=false):HomeRepository{private val q=DailyQuestion("preview-question",LocalDate.now(),"내가 가장 좋아하는 음식은?");private var h=FamilyHome(LocalDate.now(),"me",if(browsing)null else "끼리네 가족",if(browsing)emptyList() else listOf(FamilyMember("me","나","자녀"),FamilyMember("mother","마마","엄마"),FamilyMember("father","빠빠","아빠"),FamilyMember("sibling","돌돌이","자녀")),if(browsing)null else q,if(browsing)emptyMap() else mapOf("mother" to "뚝배기불고기"),emptySet(),if(browsing)null else WeeklyMission("preview-mission","광장시장에서 시장음식 먹어보기","광장시장 · 종로구",listOf("대중교통 편리","혼잡도 높음"),false,place="광장시장 · 종로구"),recommendations=if(browsing)emptyList() else listOf(HomeRecommendation("preview-place","경복궁에서 사진찍기","경복궁 종로구",listOf("대중교통 편리","가족과 함께"),"4.6",description="대중교통이 편리하고, 부모님이랑 가기 좋아요! 추천합니다")));override suspend fun load()=h;override suspend fun saveAnswer(question:DailyQuestion,answer:String):FamilyHome{h=h.copy(answers=h.answers+(h.currentUserId to answer));return h};override suspend fun poke(question:DailyQuestion,memberId:String):FamilyHome{h=h.copy(pokedMemberIds=h.pokedMemberIds+memberId);return h}}
@Preview(name="Home",showBackground=true,backgroundColor=0xFF202020,device="spec:width=411dp,height=891dp,dpi=420")
@Composable private fun HomePreview(){HomeScreen(viewModel=remember{HomeViewModel(PreviewHomeRepository())})}
@Preview(name="Home browsing",showBackground=true,backgroundColor=0xFF202020,device="spec:width=411dp,height=891dp,dpi=420")
@Composable private fun HomeBrowsingPreview(){HomeScreen(viewModel=remember{HomeViewModel(PreviewHomeRepository(true))})}
