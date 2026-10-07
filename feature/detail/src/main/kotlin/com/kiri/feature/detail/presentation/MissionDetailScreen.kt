package com.kiri.feature.detail.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.domain.detail.MissionDetail
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.*
import com.kiri.uikit.R as UiR
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun MissionDetailRoute(onBack: () -> Unit, onCustom: () -> Unit, viewModel: DetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MissionDetailScreen(state, onBack, { viewModel.beginCustom(); onCustom() }, viewModel::load)
}

@Composable
fun MissionDetailScreen(state: DetailUiState, onBack: () -> Unit, onCustom: () -> Unit, onRetry: () -> Unit = {}) {
    val detail = state.detail
    Column(Modifier.fillMaxSize().background(KiriColor.Bg.Base).testTag("mission_detail_screen").verticalScroll(rememberScrollState())) {
        if (detail == null) {
            KiriDetailAppBar("미션 상세", onBack)
            if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
            else Column(Modifier.padding(18.dp)) {
                Text(state.error ?: "미션을 찾을 수 없어요.", style = KiriDetailTypography.Body)
                KiriDetailButton("다시 시도", onRetry)
            }
        } else {
            DetailHeader(detail, onBack, onCustom)
            Spacer(Modifier.height(36.dp))
            Row(
                Modifier.align(Alignment.CenterHorizontally).background(KiriColor.Bg.Secondary, RoundedCornerShape(100.dp)).padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("끼리네 AI 활동", Modifier.background(KiriColor.Bg.Tertiary, RoundedCornerShape(30.dp)).padding(horizontal = 20.dp, vertical = 11.dp), color = KiriColor.Text.Strong, style = KiriDetailTypography.Title.copy(lineHeight = 22.sp))
                // Record feature is intentionally not connected, including its tab.
                Text("가족 추억", Modifier.padding(horizontal = 20.dp, vertical = 11.dp), color = KiriColor.Text.Disabled, style = KiriDetailTypography.Title.copy(lineHeight = 22.sp))
            }
            Spacer(Modifier.height(36.dp))
            Column(Modifier.padding(horizontal = 18.dp)) {
                SectionTitle(detail.familyName, "의 필수 활동")
                Spacer(Modifier.height(16.dp))
                if (detail.activities.isEmpty()) EmptyInfo("아직 등록된 필수 활동이 없어요.")
                detail.activities.forEachIndexed { index, activity ->
                    if (index > 0) Spacer(Modifier.height(16.dp))
                    KiriDetailSuggestionCard(activity.title, activity.description, Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(48.dp))
                SectionTitle(detail.familyName, "의 필수템")
                Spacer(Modifier.height(16.dp))
                if (detail.essentials.isEmpty()) EmptyInfo("일정에 맞는 준비물 정보가 아직 없어요.")
                detail.essentials.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { item -> KiriDetailSuggestionCard(item.title, item.description, Modifier.weight(1f), item = true) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(48.dp))
                Text("${detail.title} 근처 놀거리추천", style = KiriDetailTypography.Title, color = KiriColor.Text.Strong)
                Spacer(Modifier.height(4.dp))
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = KiriColor.Text.Accent)) { append(detail.familyName) }
                    append("한테 맞는 놀거리를 추천해드려요!")
                }, style = KiriDetailTypography.Small, color = KiriColor.Text.Tertiary)
                if (detail.conditions.isNotEmpty()) Text("컨디션에 맞게 추천 순서를 조정했어요.", Modifier.padding(top = 8.dp), style = KiriDetailTypography.Small, color = KiriColor.Text.Accent)
                Spacer(Modifier.height(16.dp))
            }
            if (state.nearby.isEmpty()) Box(Modifier.padding(horizontal = 18.dp)) { EmptyInfo("주변 추천 정보가 아직 없어요.") }
            else Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                state.nearby.forEach { place ->
                    KiriPlaceCard(place.title, place.address, place.description, rating = null, detailStyle = true)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeader(detail: MissionDetail, onBack: () -> Unit, onCustom: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(KiriColor.Bg.Primary)) {
        KiriDetailAppBar(detail.date?.let { "${it.monthValue}월 ${it.dayOfMonth}일" } ?: "미션 상세", onBack)
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 21.dp, bottom = 34.dp)) {
            val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
            val badge = when {
                detail.completed -> "완료한 미션이에요!"
                detail.date == today -> "약속 당일이에요!"
                detail.date == null -> "이번 주 미션이에요!"
                detail.date?.isBefore(today) == true -> "지난 약속이에요"
                else -> "다가오는 약속이에요!"
            }
            Text(badge, Modifier.background(if (detail.completed) KiriColor.Bg.Elevated else KiriColor.Bg.GreenSoft, RoundedCornerShape(5.dp)).padding(horizontal = 8.dp, vertical = 6.dp), style = KiriDetailTypography.Small, color = if (detail.completed) KiriColor.Text.Primary else KiriColor.Text.Accent)
            Spacer(Modifier.height(11.dp))
            Text(detail.title, color = KiriColor.Text.Strong, style = KiriDetailTypography.Display)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("참여상태  I", color = KiriColor.Text.Primary, style = KiriDetailTypography.Body)
                if (detail.participants.isEmpty()) Text("참여 정보 없음", color = KiriColor.Text.Secondary, style = KiriDetailTypography.Body)
                detail.participants.forEach { participant ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(participant.name, color = KiriColor.Text.Primary, style = KiriDetailTypography.Body)
                        if (participant.participating) Image(painterResource(UiR.drawable.kiri_detail_check), "참여", Modifier.size(18.dp))
                        else Text("미참여", color = KiriColor.Text.Tertiary, style = KiriDetailTypography.Small)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Column(Modifier.fillMaxWidth().background(KiriColor.Bg.Secondary, RoundedCornerShape(8.dp)).padding(horizontal = 26.dp, vertical = 17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoText("시작 시간   I   ${detail.startTime ?: "미정"}")
                InfoText("장소   I   ${detail.place ?: "미정"}")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    InfoText("예상 걸음수 I ${detail.stepsLabel}")
                    InfoText("활동시간 I ${detail.durationLabel}")
                }
            }
            Spacer(Modifier.height(16.dp))
            KiriDetailButton(
                text = if (detail.completed) "오늘의 추억 기록하러가기" else "컨디션 조정하러가기",
                onClick = onCustom, accent = detail.completed, enabled = !detail.completed,
                modifier = Modifier.testTag(if (detail.completed) "record_deferred" else "open_custom"),
            )
        }
    }
}

@Composable private fun InfoText(text: String) { Text(text, color = KiriColor.Text.Primary, style = KiriDetailTypography.Body) }
@Composable private fun EmptyInfo(text: String) { Text(text, color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small) }
@Composable private fun SectionTitle(family: String, suffix: String) {
    Text(buildAnnotatedString { append("오늘 "); withStyle(SpanStyle(color = KiriColor.Text.Accent)) { append(family) }; append(suffix) }, color = KiriColor.Text.Strong, style = KiriDetailTypography.Title.copy(lineHeight = 22.sp))
}
