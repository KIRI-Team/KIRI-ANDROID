package com.kiri.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.color.KiriComponentColor
import com.kiri.uikit.component.KiriInviteCode
import com.kiri.uikit.component.KiriAppBar

@Composable
fun InviteFamilyScreen(inviteCode: String, onBack: () -> Unit, onShare: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxSize().background(HomeBackground).padding(vertical = 8.dp)) {
        KiriAppBar("", onBack = onBack)
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("가족코드를\n전달해주세요", color = HomeStrong, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp)); Text("복사해서 가족에게 보내주세요", color = HomeSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(36.dp))
            KiriInviteCode(inviteCode, onCopy = { clipboard.setText(AnnotatedString(inviteCode)) })
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(54.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = HomeSecondary)) { Text("취소") }
            Button(onClick = onShare, modifier = Modifier.weight(1f).height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = KiriComponentColor.Button.FilledAccent.BgDefault, contentColor = KiriComponentColor.Button.FilledAccent.ContentDefault)) { Text("링크 공유하기", fontWeight = FontWeight.SemiBold) }
        }
    }
}
