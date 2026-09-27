package com.kiri.feature.mypage.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun MypageScreen(onDetailClick: () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().testTag("mypage_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("마이페이지")
        Text("화면 준비 중")
        Button(onClick = onDetailClick) { Text("상세보기") }
    }
}
