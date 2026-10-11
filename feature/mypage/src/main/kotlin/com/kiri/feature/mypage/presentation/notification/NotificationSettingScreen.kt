package com.kiri.feature.mypage.presentation.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.mypage.NotificationSettingViewModel
import com.kiri.feature.mypage.model.notification.NotificationSettingUiEffect
import com.kiri.feature.mypage.model.notification.NotificationSettingUiEvent
import com.kiri.feature.mypage.model.notification.NotificationSettingUiState
import com.kiri.feature.mypage.presentation.common.DISABLED_ALPHA
import com.kiri.feature.mypage.presentation.common.MypageToggle
import com.kiri.uikit.R as UikitR
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.color.KiriComponentColor
import com.kiri.uikit.component.KiriTopBar

@Composable
fun NotificationSettingRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationSettingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                NotificationSettingUiEffect.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    NotificationSettingScreen(modifier = modifier, state = state, onEvent = viewModel::onEvent)
}

// 알림 설정. 전체 알림과 정기 알림(발송 시각)을 켜고 끈다
@Composable
fun NotificationSettingScreen(
    state: NotificationSettingUiState,
    onEvent: (NotificationSettingUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .testTag("notification_setting_screen"),
    ) {
        KiriTopBar(onBackClick = { onEvent(NotificationSettingUiEvent.BackClicked) })
        Column(
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ToggleRow(
                title = "전체 알림 설정",
                checked = state.allEnabled,
                onToggle = { onEvent(NotificationSettingUiEvent.AllToggled) },
                testTag = "notification_all_toggle",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            )
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ToggleRow(
                    title = "정기 알림 시간 설정",
                    checked = state.regularEnabled,
                    onToggle = { onEvent(NotificationSettingUiEvent.RegularToggled) },
                    testTag = "notification_regular_toggle",
                    enabled = state.canChangeRegular,
                )
                RegularTimeRow(
                    timeText = state.regularTimeText,
                    enabled = state.canChangeRegularTime,
                    onClick = { onEvent(NotificationSettingUiEvent.RegularTimeClicked) },
                )
            }
            Text(
                text = "푸시 알림을 꺼도 앱 안의 알림함에서 소식을 확인할 수 있어요.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = KiriColor.Text.Tertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.sp,
                    letterSpacing = (-0.04).em,
                ),
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            modifier = Modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
            style = TextStyle(
                color = KiriColor.Text.Strong,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 27.sp,
                letterSpacing = (-0.04).em,
            ),
        )
        MypageToggle(
            checked = checked,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            testTag = testTag,
        )
    }
}

@Composable
private fun RegularTimeRow(
    timeText: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(RoundedCornerShape(8.dp))
            .background(KiriComponentColor.Button.FilledNeutral.BgDefault)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("notification_regular_time"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "현재 알림 발송 시각 : $timeText",
            style = TextStyle(
                color = KiriColor.Text.Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 21.sp,
                letterSpacing = (-0.04).em,
            ),
        )
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            // Figma처럼 뒤로가기 화살표를 좌우 반전하고 강조색으로 칠한다
            Image(
                painter = painterResource(UikitR.drawable.ic_kiri_back_arrow),
                contentDescription = null,
                colorFilter = ColorFilter.tint(KiriColor.Icon.Accent),
                modifier = Modifier.scale(scaleX = -1f, scaleY = 1f),
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun NotificationSettingScreenPreview() {
    NotificationSettingScreen(state = NotificationSettingUiState(), onEvent = {})
}
