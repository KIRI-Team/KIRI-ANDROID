package com.kiri.app.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.R
import com.kiri.uikit.component.KiriDetailTypography

/** Figma 8389:5585. Keep selection tied to the main back stack. */
@Composable
fun KiriBottomBar(
    navController: NavHostController,
    // 이 탭으로 가지 못하게 막을지 (둘러보기 사용자의 기록 탭 등). 막으면 이동하지 않고 onBlocked 를 부른다
    isBlocked: (TopLevelDestination) -> Boolean = { false },
    onBlocked: (TopLevelDestination) -> Unit = {},
) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    Box(
        Modifier.fillMaxWidth().testTag("bottom_nav_area").navigationBarsPadding().padding(horizontal = 19.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 352.dp).fillMaxWidth().testTag("floating_bottom_bar")
                .border(0.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .4f), Color.White.copy(alpha = .08f))), RoundedCornerShape(100.dp)),
            shape = RoundedCornerShape(100.dp), color = KiriColor.Bg.Elevated, shadowElevation = 8.dp,
        ) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp).selectableGroup()) {
                listOf(TopLevelDestination.HOME, TopLevelDestination.PLACE, TopLevelDestination.RECORD, TopLevelDestination.MY_PAGE).forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.graphRoute } == true
                    Surface(
                        color = if (selected) KiriColor.Bg.Tertiary else Color.Transparent,
                        shape = RoundedCornerShape(100.dp), modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            Modifier.clip(RoundedCornerShape(100.dp)).selectable(selected = selected, role = Role.Tab, onClick = {
                                if (isBlocked(destination)) return@selectable onBlocked(destination)
                                navController.navigate(destination.graphRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }).testTag("bottom_nav_${destination.startRoute}").padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val icon = when (destination) {
                                TopLevelDestination.HOME -> if (selected) R.drawable.kiri_nav_home else R.drawable.kiri_nav_home_disabled
                                TopLevelDestination.PLACE -> if (selected) R.drawable.kiri_nav_place else R.drawable.kiri_nav_place_disabled
                                TopLevelDestination.RECORD -> if (selected) R.drawable.kiri_nav_record else R.drawable.kiri_nav_record_disabled
                                TopLevelDestination.MY_PAGE -> if (selected) R.drawable.kiri_nav_profile else R.drawable.kiri_nav_profile_disabled
                            }
                            Image(painterResource(icon), null, Modifier.size(24.dp))
                            Text(
                                if (destination == TopLevelDestination.PLACE) "플레이스" else destination.label,
                                color = if (selected) KiriColor.Text.Accent else KiriColor.Text.Disabled,
                                style = KiriDetailTypography.Small.copy(
                                    fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = (-.48).sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                ), maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
