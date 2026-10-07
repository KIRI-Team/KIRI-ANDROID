package com.kiri.app.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kiri.uikit.color.KiriColor

@Composable
fun KiriBottomBar(navController: NavHostController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    NavigationBar(containerColor = KiriColor.Bg.Primary, contentColor = KiriColor.Text.Strong) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.graphRoute } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.graphRoute) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = KiriColor.Icon.Accent,
                    selectedTextColor = KiriColor.Text.Strong,
                    unselectedIconColor = KiriColor.Icon.Secondary,
                    unselectedTextColor = KiriColor.Text.Secondary,
                    indicatorColor = KiriColor.Bg.Secondary,
                ),
                modifier = Modifier.testTag("bottom_nav_${destination.startRoute}"),
            )
        }
    }
}
