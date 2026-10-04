package com.kiri.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InviteFamilyScreen(inviteCode: String = "NWTKYJ", onBack: () -> Unit, onShare: () -> Unit) {
    Column(Modifier.fillMaxSize().background(HomeBackground).padding(horizontal = 18.dp, vertical = 8.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.size(52.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = HomeStrong) }
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("가족코드를\n전달해주세요", color = HomeStrong, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp)); Text("복사해서 가족에게 보내주세요", color = HomeSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(36.dp))
            Surface(Modifier.fillMaxWidth(), color = HomeCard, shape = RoundedCornerShape(8.dp)) { Column(Modifier.padding(vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("링크코드", color = HomeSecondary, fontSize = 12.sp); Text(inviteCode, color = HomeStrong, fontSize = 20.sp, letterSpacing = 2.sp) } }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(54.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = HomeSecondary)) { Text("취소") }
            Button(onClick = onShare, modifier = Modifier.weight(1f).height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = HomeAccent, contentColor = HomeBackground)) { Text("링크 공유하기", fontWeight = FontWeight.SemiBold) }
        }
    }
}
