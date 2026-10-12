package com.kiri.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.kiri.feature.home.R
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kiri.domain.notification.*
import com.kiri.feature.home.NotificationUiState
import com.kiri.feature.home.NotificationViewModel
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.*
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NotificationRoute(onBack: () -> Unit, onTarget: (FamilyNotification) -> Unit, viewModel: NotificationViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTarget by rememberUpdatedState(onTarget)
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.target.collect { currentTarget(it) } }
    }
    NotificationScreen(state, onBack, viewModel::select, viewModel::readAll, viewModel::open, viewModel::reload)
}

@Composable
fun NotificationScreen(state: NotificationUiState, onBack: () -> Unit, onSelect: (NotificationCategory) -> Unit, onReadAll: () -> Unit, onOpen: (FamilyNotification) -> Unit, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().background(KiriColor.Bg.Base).testTag("notification_screen")) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
            KiriLeadingIconSlot(onBack)
            Text("알림함${if (state.unreadCount > 0) " +${if (state.unreadCount > 99) "99" else state.unreadCount}" else ""}", Modifier.weight(1f).padding(start = 11.dp), style = KiriDetailTypography.Title.copy(fontSize = 20.sp, lineHeight = 30.sp), color = KiriColor.Text.Primary)
            TextButton(onReadAll, enabled = state.unreadCount > 0 && !state.busy && !state.loading, modifier = Modifier.padding(end = 10.dp).testTag("notifications_read_all")) {
                Text("모두읽음", color = if (state.unreadCount > 0) KiriColor.Text.Primary else KiriColor.Text.Disabled, style = KiriDetailTypography.Small)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 21.dp, end = 18.dp, top = 21.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NotificationCategory.entries.forEach { category ->
                val selected = state.selected == category
                Box(Modifier.height(36.dp).background(if (selected) KiriColor.Primary.Default.copy(alpha = .2f) else KiriColor.Bg.Tertiary, RoundedCornerShape(100.dp))
                    .selectable(selected, role = Role.Tab) { onSelect(category) }.testTag("notification_filter_${category.name}").padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text(category.label, color = if (selected) KiriColor.Text.Accent else KiriColor.Text.Secondary, style = KiriDetailTypography.Small.copy(fontWeight = FontWeight.Medium, lineHeight = 20.sp))
                }
            }
        }
        HorizontalDivider(color = KiriColor.Divider)
        state.notice?.let { Text(it, Modifier.padding(18.dp).testTag("notification_notice"), style = KiriDetailTypography.Small, color = KiriColor.Text.Accent) }
        when {
            state.loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error, style = KiriDetailTypography.Body, color = KiriColor.Text.Secondary)
                TextButton(onRetry, Modifier.testTag("notifications_retry")) { Text("다시 시도", color = KiriColor.Text.Accent) }
            }
            state.visibleItems.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("아직 도착한 알림이 없어요", Modifier.testTag("notifications_empty"), color = KiriColor.Text.Tertiary, style = KiriDetailTypography.Body)
            }
            else -> LazyColumn(Modifier.weight(1f).testTag("notification_list"), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(state.visibleItems, key = { it.id }) { notification ->
                    NotificationRow(notification, !state.busy) { onOpen(notification) }
                    HorizontalDivider(color = KiriColor.Divider)
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: FamilyNotification, enabled: Boolean, onClick: () -> Unit) {
    val bodyColor = if (item.read) KiriColor.Text.Disabled else KiriColor.Text.Tertiary
    Column(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).testTag("notification_${item.id}").padding(horizontal = 18.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.sender, color = if (item.read) bodyColor else KiriColor.Text.Secondary, style = KiriDetailTypography.Small.copy(fontSize = 12.sp, lineHeight = 18.sp))
            Text(notificationTime(item.createdAt), color = bodyColor, style = KiriDetailTypography.Small.copy(fontSize = 12.sp, lineHeight = 18.sp))
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.title, Modifier.weight(1f, fill = false), color = if (item.read) KiriColor.Text.Disabled else KiriColor.Text.Strong, style = KiriDetailTypography.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.id == "demo-invite") {
                Spacer(Modifier.width(4.dp))
                Image(painterResource(R.drawable.notification_invite_envelope), null, Modifier.size(width = 21.744.dp, height = 11.268.dp))
            }
        }
        Text(item.message, color = bodyColor, style = KiriDetailTypography.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun notificationTime(createdAt: Instant): String {
    val elapsed = Duration.between(createdAt, Instant.now())
    return when {
        elapsed.toMinutes() < 1 -> "방금 전"
        elapsed.toHours() < 1 -> "${elapsed.toMinutes()}분 전"
        elapsed.toDays() < 1 -> "${elapsed.toHours()}시간 전"
        elapsed.toDays() < 7 -> "${elapsed.toDays()}일 전"
        else -> createdAt.atZone(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("M월 d일"))
    }
}
