package com.kiri.feature.home.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.domain.home.*
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

enum class HomeUiDestination { CreateFamily, JoinFamily, InviteFamily, Schedule, AnswerHistory, Mission }

@Composable
fun HomeScreen(onDetailClick: () -> Unit = {}, onDestination: (HomeUiDestination) -> Unit = {}, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val home = state.home
    val memberOffsets = remember { mutableStateMapOf<String, IntOffset>() }
    Box(Modifier.fillMaxSize().background(HomeBackground)) {
        if (state.editing) {
            val editingHome = home ?: return@Box
            val question = editingHome.question ?: return@Box
            AnswerEditor(question.text, state.draft, state.busy, state.error,
                { viewModel.onAction(HomeAction.ChangeAnswer(it)) }, { viewModel.onAction(HomeAction.SaveAnswer) }, { viewModel.onAction(HomeAction.Back) })
            if (state.answerSaved) AnswerSavedDialog { viewModel.onAction(HomeAction.DismissNotice) }
            return@Box
        }
        Column(Modifier.fillMaxSize().testTag("home_screen").statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(home?.familyName ?: "둘러보기", Modifier.weight(1f), color = HomeStrong, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { viewModel.onAction(HomeAction.Retry) }, enabled = !state.loading && !state.busy) { Icon(Icons.Default.Cached, "새로고침", tint = HomeSecondary) }
            }
            Spacer(Modifier.height(18.dp))
            when {
                state.loading -> LoadingHome()
                state.loadError || home == null -> ErrorHome { viewModel.onAction(HomeAction.Retry) }
                else -> {
                    selectHomeSchedule(home.schedules, Instant.now(), ZoneId.of("Asia/Seoul"))?.let { ScheduleCard(it, onDestination) }
                    if (home.browsing) BrowseCard(onDestination) else {
                        AnswersHeader { onDestination(HomeUiDestination.InviteFamily) }
                        StoryRow(home, viewModel::onAction, memberOffsets)
                    }
                    Spacer(Modifier.height(26.dp))
                    QuestionMissionSection(home, state.showingMission, viewModel::onAction)
                    if (!home.browsing) RecommendationSection(home.recommendations)
                    TextButton(onClick = onDetailClick) { Text("상세보기", color = HomeSecondary) }
                }
            }
        }
        val selectedMemberId = state.selectedMemberId
        if (selectedMemberId != null && home != null) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.58f)).clickable(enabled = !state.busy) { viewModel.onAction(HomeAction.Back) })
            MemberAnswerToast(home, selectedMemberId, state.busy, memberOffsets[selectedMemberId], viewModel::onAction)
        }
        state.notice?.let { NoticeBar(it) { viewModel.onAction(HomeAction.DismissNotice) } }
    }
}

@Composable private fun LoadingHome() { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) { repeat(4) { Box(Modifier.fillMaxWidth().height(if (it == 0) 48.dp else 110.dp).clip(RoundedCornerShape(16.dp)).background(HomeCard)) } } }
@Composable private fun ErrorHome(onRetry: () -> Unit) { Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("홈을 불러오지 못했어요.", color = HomeStrong); Spacer(Modifier.height(14.dp)); Button(onClick = onRetry) { Text("다시 시도") } } }

@Composable private fun AnswersHeader(onInvite: () -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("답변 모아보기", Modifier.weight(1f), color = HomeStrong, fontSize = 18.sp); OutlinedButton(onClick = onInvite, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp), modifier = Modifier.height(32.dp)) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(2.dp)); Text("가족 초대하기", fontSize = 11.sp) } } }

@Composable private fun BrowseCard(onDestination: (HomeUiDestination) -> Unit) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(HomeCard), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("가족과 함께하는 질문을 시작해보세요", color = HomeStrong, fontSize = 18.sp); Text("가족공간을 만들거나 초대코드로 참여하면 오늘의 질문을 받을 수 있어요.", color = HomeSecondary, fontSize = 14.sp); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { onDestination(HomeUiDestination.CreateFamily) }) { Text("가족 만들기") }; Button(onClick = { onDestination(HomeUiDestination.JoinFamily) }) { Text("가족 참여하기") } } } } }

@Composable private fun StoryRow(home: FamilyHome, onAction: (HomeAction) -> Unit, offsets: MutableMap<String, IntOffset>) { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) { home.members.forEach { member -> val answered = home.question != null && member.id in home.answers; val border = if (answered) HomeAccent else HomeCard; Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(52.dp)) { Surface(onClick = { onAction(HomeAction.SelectMember(member.id)) }, enabled = member.accessible, shape = CircleShape, border = BorderStroke(1.dp, border), color = HomeCard, modifier = Modifier.size(46.dp).onGloballyPositioned { coordinates: LayoutCoordinates -> val position = coordinates.positionInRoot(); offsets[member.id] = IntOffset(position.x.roundToInt(), (position.y + coordinates.size.height).roundToInt() + 8) }) { Box(contentAlignment = Alignment.Center) { Text(if (member.id == home.currentUserId) "나" else member.role.take(1), color = if (answered) HomeAccent else HomeSecondary, fontWeight = FontWeight.Bold) } }; Text(if (member.id == home.currentUserId) "내 답변" else member.nickname, color = HomeSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } } } }

@Composable private fun QuestionMissionSection(home: FamilyHome, showingMission: Boolean, onAction: (HomeAction) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) { Text(if (showingMission) "이번주 미션이에요!" else "오늘 나의 질문카드", Modifier.weight(1f), color = HomeStrong, fontSize = 18.sp); Text("다른 가족들의 답변을 기다려요!", color = HomeSecondary, fontSize = 11.sp) }; Spacer(Modifier.height(12.dp)); QuestionMissionCard(home, showingMission, onAction) }

@Composable private fun QuestionMissionCard(home: FamilyHome, showingMission: Boolean, onAction: (HomeAction) -> Unit) { var dragDistance by remember(showingMission) { mutableFloatStateOf(0f) }; val modifier = Modifier.fillMaxWidth().pointerInput(showingMission) { detectHorizontalDragGestures(onHorizontalDrag = { change, amount -> change.consume(); dragDistance += amount }, onDragEnd = { if (kotlin.math.abs(dragDistance) > 70f) onAction(HomeAction.Flip); dragDistance = 0f }, onDragCancel = { dragDistance = 0f }) }.clickable(enabled = !showingMission && home.question != null) { onAction(HomeAction.EditAnswer) }.testTag(if (showingMission) "home_mission_card" else "home_question_card"); Card(modifier, colors = CardDefaults.cardColors(HomeCard), shape = RoundedCornerShape(16.dp)) { AnimatedContent(showingMission, label = "question-mission-swipe") { if (it) MissionCardContent(home.mission) else QuestionCardContent(home) } } }

@Composable private fun QuestionCardContent(home: FamilyHome) { val question = home.question; Column(Modifier.padding(16.dp).heightIn(min = 108.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("오늘의 질문에 답변해주세요!", color = HomeSecondary, fontSize = 12.sp); if (question == null) Text("오늘의 질문이 아직 배정되지 않았어요.", color = HomeSecondary) else { Text("Q1. ${question.text}", color = HomeStrong, fontSize = 17.sp); home.answers[home.currentUserId]?.let { Text("A. $it", color = HomeAccent, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) } } } }

@Composable private fun MissionCardContent(mission: WeeklyMission?) { if (mission == null) { Text("이번 주 미션이 없어요.", Modifier.padding(20.dp), color = HomeSecondary); return }; Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(86.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF454545)), contentAlignment = Alignment.Center) { Text("사진 준비 중", color = HomeSecondary, fontSize = 11.sp) }; Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(mission.title, color = HomeStrong, fontSize = 17.sp, fontWeight = FontWeight.SemiBold); Text(mission.place ?: mission.description, color = HomeSecondary, fontSize = 13.sp); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { mission.keywords.forEach { Surface(color = HomeCard, shape = RoundedCornerShape(6.dp)) { Text(it, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = HomeSecondary, fontSize = 11.sp) } } } } } }

@Composable private fun RecommendationSection(recommendations: List<HomeRecommendation>) { if (recommendations.isEmpty()) return; Column(Modifier.padding(top = 28.dp)) { Text("엄마와 함께라면 여기!", color = HomeStrong, fontSize = 18.sp); Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) { recommendations.forEach { recommendation -> Card(Modifier.width(202.dp), colors = CardDefaults.cardColors(Color(0xFF404040)), shape = RoundedCornerShape(16.dp)) { Column { Box(Modifier.fillMaxWidth().height(116.dp).background(Color(0xFF555555)), contentAlignment = Alignment.Center) { Text("사진 준비 중", color = HomeSecondary, fontSize = 11.sp) }; Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(recommendation.title, Modifier.weight(1f), color = HomeStrong, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); recommendation.rating?.let { Text("★ $it", color = HomeSecondary, fontSize = 11.sp) } }; Text(recommendation.place, color = HomeSecondary, fontSize = 12.sp); recommendation.keywords.forEach { Text("· $it", color = HomeSecondary, fontSize = 11.sp) } } } } } } } }

@Composable private fun ScheduleCard(card: HomeScheduleCard, onDestination: (HomeUiDestination) -> Unit) { val label = when (card.kind) { HomeScheduleCard.Kind.RESPONSE_REQUIRED -> "응답이 필요한 일정"; HomeScheduleCard.Kind.TODAY -> "오늘의 일정"; HomeScheduleCard.Kind.UPCOMING -> "다가오는 일정" }; Card(onClick = { onDestination(HomeUiDestination.Schedule) }, Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(HomeCard), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(label, color = HomeAccent, fontSize = 14.sp); Text(card.schedule.title, color = HomeStrong, fontSize = 18.sp) }; Icon(Icons.Default.ChevronRight, "일정 열기", tint = HomeSecondary) } } }

@Composable private fun MemberAnswerToast(home: FamilyHome, memberId: String, busy: Boolean, offset: IntOffset?, onAction: (HomeAction) -> Unit) { val member = home.members.firstOrNull { it.id == memberId } ?: return; Popup(alignment = Alignment.TopStart, offset = offset ?: IntOffset(18, 260), properties = PopupProperties(focusable = false)) { Card(Modifier.width(226.dp), colors = CardDefaults.cardColors(Color(0xFF414141)), shape = RoundedCornerShape(14.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("${member.nickname}의 가장 좋아하는 음식은", color = HomeStrong, fontSize = 12.sp); Text(home.answers[member.id] ?: "아직 답변하지 않았어요", color = HomeStrong, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }; if (home.canPoke(member.id)) IconButton(onClick = { onAction(HomeAction.Poke) }, enabled = !busy) { Icon(Icons.Default.TouchApp, "콕 찌르기", tint = HomeAccent) } } } } }

@Composable private fun NoticeBar(message: String, onDismiss: () -> Unit) { Snackbar(Modifier.padding(16.dp), action = { TextButton(onClick = onDismiss) { Text("닫기") } }) { Text(message) } }

private class PreviewHomeRepository(private val browsing: Boolean = false) : HomeRepository { private val question = DailyQuestion("preview-question", java.time.LocalDate.of(2026, 10, 3), "오늘 가족과 함께하고 싶은 일은 무엇인가요?"); private var home = FamilyHome(question.date, "me", if (browsing) null else "우리 가족", if (browsing) emptyList() else listOf(FamilyMember("me", "나", "자녀"), FamilyMember("mom", "엄마", "엄마"), FamilyMember("dad", "아빠", "아빠")), if (browsing) null else question, if (browsing) emptyMap() else mapOf("mom" to "같이 산책하고 싶어요."), emptySet(), if (browsing) null else WeeklyMission("preview-mission", "광장시장에서 시장음식 먹어보기", "광장시장 · 종로구", listOf("대중교통 편리", "혼잡도 높음"), false, place = "광장시장 · 종로구"), recommendations = if (browsing) emptyList() else listOf(HomeRecommendation("preview-place", "경복궁에서 사진찍기", "경복궁 · 종로구", listOf("대중교통 편리"), "4.6"))); override suspend fun load() = home; override suspend fun saveAnswer(question: DailyQuestion, answer: String): FamilyHome { home = home.copy(answers = home.answers + (home.currentUserId to answer)); return home }; override suspend fun poke(question: DailyQuestion, memberId: String): FamilyHome { home = home.copy(pokedMemberIds = home.pokedMemberIds + memberId); return home } }

@Preview(name = "Home", showBackground = true, backgroundColor = 0xFF202020, device = "spec:width=411dp,height=891dp,dpi=420")
@Composable private fun HomePreview() { HomeScreen(viewModel = remember { HomeViewModel(PreviewHomeRepository()) }) }

@Preview(name = "Home browsing", showBackground = true, backgroundColor = 0xFF202020, device = "spec:width=411dp,height=891dp,dpi=420")
@Composable private fun HomeBrowsingPreview() { HomeScreen(viewModel = remember { HomeViewModel(PreviewHomeRepository(browsing = true)) }) }
