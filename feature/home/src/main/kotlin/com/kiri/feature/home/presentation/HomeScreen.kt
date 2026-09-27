package com.kiri.feature.home.presentation

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
fun HomeScreen(onDetailClick: () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().testTag("home_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("홈")
        Text("화면 준비 중")
        Button(onClick = onDetailClick) { Text("상세보기") }
    }
}
