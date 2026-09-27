package com.kiri.feature.place.presentation

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
fun PlaceScreen(onDetailClick: () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().testTag("place_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("장소")
        Text("화면 준비 중")
        Button(onClick = onDetailClick) { Text("상세보기") }
    }
}
