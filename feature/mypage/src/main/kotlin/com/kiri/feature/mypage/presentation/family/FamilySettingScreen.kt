package com.kiri.feature.mypage.presentation.family

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.mypage.FamilySettingViewModel
import com.kiri.feature.mypage.R
import com.kiri.feature.mypage.model.common.MypageMenu
import com.kiri.feature.mypage.model.family.FamilyMember
import com.kiri.feature.mypage.model.family.FamilySettingUiState
import com.kiri.feature.mypage.presentation.family.component.FamilyMemberCard
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriTopBar

// 가족 정보 API 로 가족 이름과 구성원을 채운다. 가족명·닉네임을 바꾸고 돌아오면 다시 불러온다
@Composable
fun FamilySettingRoute(
    onBackClick: () -> Unit,
    onFamilyNameEditClick: () -> Unit,
    onMyNicknameEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FamilySettingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    FamilySettingScreen(
        onBackClick = onBackClick,
        onFamilyNameEditClick = onFamilyNameEditClick,
        onMyNicknameEditClick = onMyNicknameEditClick,
        modifier = modifier,
        state = state,
        onRetry = viewModel::load,
    )
}

// Figma 10298:38226 가족 기본 설정. 가족 이름과 구성원(캐릭터·닉네임·역할) 목록을 보여주고,
// 수정(펜) 아이콘으로 가족명·내 닉네임 수정 화면을 연다
@Composable
fun FamilySettingScreen(
    onBackClick: () -> Unit,
    onFamilyNameEditClick: () -> Unit,
    onMyNicknameEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: FamilySettingUiState = FamilySettingUiState(loading = false),
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .testTag("family_setting_screen"),
    ) {
        KiriTopBar(onBackClick = onBackClick, title = MypageMenu.FAMILY_SETTING.title)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(36.dp))
            SectionTitle("가족 닉네임 관리")
            Spacer(Modifier.height(16.dp))
            FamilyNameCard(state.familyName, state.canRenameFamily, onFamilyNameEditClick)
            Spacer(Modifier.height(56.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("가족 구성원 관리")
                if (state.members.isNotEmpty()) {
                    Text(
                        text = state.members.size.toString(),
                        modifier = Modifier.testTag("family_member_count"),
                        style = TextStyle(
                            color = KiriColor.Text.Primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 23.sp,
                            letterSpacing = (-0.04).em,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            when {
                state.loading -> CircularProgressIndicator(
                    Modifier.align(Alignment.CenterHorizontally).padding(24.dp).testTag("family_setting_loading"),
                    color = KiriColor.Text.Accent,
                )
                state.failed -> Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoText("가족 정보를 불러오지 못했어요.")
                    Text(
                        text = "다시 시도",
                        modifier = Modifier.clickable(onClick = onRetry).padding(vertical = 4.dp).testTag("family_setting_retry"),
                        style = TextStyle(color = KiriColor.Text.Accent, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    )
                }
                state.members.isEmpty() -> InfoText("가족에 참여하면 구성원을 볼 수 있어요.")
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.members.forEach { member ->
                        FamilyMemberCard(member = member, onEditClick = onMyNicknameEditClick)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// Figma 10298:38232 현재 가족 이름 카드 (배경 Bg.Primary, 모서리 8, 여백 16·18)
@Composable
private fun FamilyNameCard(familyName: String?, canRename: Boolean, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(KiriColor.Bg.Primary, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "현재 가족 이름",
            modifier = Modifier.weight(1f),
            style = TextStyle(
                color = KiriColor.Text.Primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.04).em,
            ),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = familyName ?: "-",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = KiriColor.Text.Strong,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.04).em,
                ),
            )
            // 가족명은 방장만 바꿀 수 있어 방장에게만 수정 아이콘을 보여준다
            if (canRename) {
                Image(
                    painter = painterResource(R.drawable.ic_mypage_edit),
                    contentDescription = "가족명 수정",
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onEditClick)
                        .testTag("family_name_edit"),
                )
            }
        }
    }
}

@Composable
private fun InfoText(text: String) {
    Text(text, style = TextStyle(color = KiriColor.Text.Secondary, fontSize = 14.sp, letterSpacing = (-0.04).em))
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = TextStyle(
            color = KiriColor.Text.Strong,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.04).em,
        ),
    )
}

@Preview(showSystemUi = true)
@Composable
private fun FamilySettingScreenPreview() {
    FamilySettingScreen(
        onBackClick = {}, onFamilyNameEditClick = {}, onMyNicknameEditClick = {},
        state = FamilySettingUiState(
            loading = false,
            familyName = "끼리네 가족",
            members = listOf(
                FamilyMember("아빠", "아빠다", isMe = true, id = "1"),
                FamilyMember("엄마", "엄마다", id = "2"),
                FamilyMember("", "막내", id = "3"),
            ),
        ),
    )
}
