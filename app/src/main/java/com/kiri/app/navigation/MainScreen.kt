package com.kiri.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kiri.uikit.color.KiriColor

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = KiriColor.Bg.Base,
        contentColor = KiriColor.Text.Strong,
        bottomBar = { KiriBottomBar(navController) },
    ) { padding ->
        KiriNavHost(navController, Modifier.padding(padding))
    }
}
