package com.kiri.feature.record.presentation

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
fun RecordScreen(onDetailClick: () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().testTag("record_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("기록")
        Text("화면 준비 중")
        Button(onClick = onDetailClick) { Text("상세보기") }
    }
}
