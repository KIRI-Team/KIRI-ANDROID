package com.kiri.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector
import com.kiri.feature.mypage.navigation.MYPAGE_GRAPH_ROUTE
import com.kiri.feature.mypage.navigation.MYPAGE_ROUTE

enum class TopLevelDestination(
    val graphRoute: String,
    val startRoute: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(graphRoute = "home_graph", startRoute = "home", label = "홈", icon = Icons.Default.Home),
    RECORD(graphRoute = "record_graph", startRoute = "record", label = "기록", icon = Icons.Default.Edit),
    PLACE(graphRoute = "place_graph", startRoute = "place", label = "장소", icon = Icons.Default.Place),
    MY_PAGE(graphRoute = MYPAGE_GRAPH_ROUTE, startRoute = MYPAGE_ROUTE, label = "마이페이지", icon = Icons.Default.Person);

    fun detailRoute() = "$graphRoute/detail"

    // 가족 공간이 있어야 쓸 수 있는 탭 (둘러보기 사용자는 막는다)
    val requiresFamily: Boolean get() = this == RECORD
}
