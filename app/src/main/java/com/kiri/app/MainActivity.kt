package com.kiri.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kiri.feature.auth.presentation.AuthScreen
import com.kiri.feature.detail.presentation.DetailScreen
import com.kiri.feature.home.presentation.HomeScreen
import com.kiri.feature.mypage.presentation.MypageScreen
import com.kiri.feature.onboarding.presentation.OnboardingScreen
import com.kiri.feature.place.presentation.PlaceScreen
import com.kiri.feature.record.presentation.RecordScreen
import com.kiri.uikit.KiriTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KiriTheme { ScreenGallery() } }
    }
}

// 디자인 작업 전, 각 feature의 화면을 직접 확인하는 임시 메뉴.
@Composable
private fun ScreenGallery() {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    if (selected == null) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("끼리 화면 미리보기")
            listOf("home", "auth", "onboarding", "detail", "record", "place", "mypage").forEach { name ->
                Button(onClick = { selected = name }) { Text(name) }
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Button(onClick = { selected = null }) { Text("목록으로") }
            when (selected) {
                "home" -> HomeScreen()
                "auth" -> AuthScreen()
                "onboarding" -> OnboardingScreen()
                "detail" -> DetailScreen()
                "record" -> RecordScreen()
                "place" -> PlaceScreen()
                "mypage" -> MypageScreen()
            }
        }
    }
}
