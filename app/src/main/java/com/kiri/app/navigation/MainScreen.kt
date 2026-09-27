package com.kiri.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(bottomBar = { KiriBottomBar(navController) }) { padding ->
        KiriNavHost(navController, Modifier.padding(padding))
    }
}
