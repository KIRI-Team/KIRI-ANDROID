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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.feature.mypage.R
import com.kiri.feature.mypage.model.common.MypageMenu
import com.kiri.feature.mypage.model.common.MypageProfile
import com.kiri.feature.mypage.model.family.FamilyMember
import com.kiri.feature.mypage.presentation.family.component.FamilyMemberCard
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriTopBar

private const val MEMBER_COLUMNS = 3

// 가족 기본 설정. 가족 이름과 구성원을 보여주고, 수정(펜) 아이콘으로 가족명·내 닉네임 수정 화면을 연다
@Composable
fun FamilySettingScreen(
    onBackClick: () -> Unit,
    onFamilyNameEditClick: () -> Unit,
    onMyNicknameEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    // TODO: 가족 정보 저장소가 생기면 실제 가족명과 구성원으로 교체
    familyName: String = MypageProfile.Sample.familyName,
    members: List<FamilyMember> = FamilyMember.Samples,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KiriColor.Bg.Secondary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "현재 가족 이름",
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
                        text = familyName,
                        style = TextStyle(
                            color = KiriColor.Text.Strong,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.04).em,
                        ),
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_mypage_edit_24),
                        contentDescription = "가족명 수정",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onFamilyNameEditClick)
                            .testTag("family_name_edit"),
                    )
                }
            }
            Spacer(Modifier.height(54.dp))
            SectionTitle("가족 구성원 관리")
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                members.chunked(MEMBER_COLUMNS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { member ->
                            FamilyMemberCard(
                                member = member,
                                onEditClick = onMyNicknameEditClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // 마지막 줄이 덜 차도 카드 너비를 같게 유지한다
                        repeat(MEMBER_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
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
    FamilySettingScreen(onBackClick = {}, onFamilyNameEditClick = {}, onMyNicknameEditClick = {})
}
