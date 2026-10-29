package com.kiri.feature.mypage.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.kiri.feature.mypage.model.common.MypageMenu
import com.kiri.feature.mypage.presentation.account.AccountInfoRoute
import com.kiri.feature.mypage.presentation.address.HomeAddressRoute
import com.kiri.feature.mypage.presentation.family.FamilySettingScreen
import com.kiri.feature.mypage.presentation.invite.InviteLinkRoute
import com.kiri.feature.mypage.presentation.license.OpenLicenseRoute
import com.kiri.feature.mypage.presentation.main.MypageScreen
import com.kiri.feature.mypage.presentation.nameedit.FamilyNameEditRoute
import com.kiri.feature.mypage.presentation.nameedit.NicknameEditRoute
import com.kiri.feature.mypage.presentation.notification.NotificationSettingRoute
import com.kiri.feature.mypage.presentation.preference.MyPreferenceScreen
import com.kiri.feature.mypage.presentation.suggestion.SuggestionRoute
import com.kiri.feature.mypage.presentation.terms.TermsRoute
import com.kiri.feature.mypage.presentation.withdraw.WithdrawRoute

const val MYPAGE_GRAPH_ROUTE = "mypage_graph"
const val MYPAGE_ROUTE = "mypage"
const val NICKNAME_EDIT_ROUTE = "$MYPAGE_GRAPH_ROUTE/nickname_edit"
const val FAMILY_NAME_EDIT_ROUTE = "$MYPAGE_GRAPH_ROUTE/family_name_edit"
const val TERMS_ROUTE = "$MYPAGE_GRAPH_ROUTE/account_info/terms"
const val OPEN_LICENSE_ROUTE = "$MYPAGE_GRAPH_ROUTE/account_info/open_license"
const val WITHDRAW_ROUTE = "$MYPAGE_GRAPH_ROUTE/account_info/withdraw"

private fun mypageMenuRoute(menu: MypageMenu) = "$MYPAGE_GRAPH_ROUTE/${menu.name.lowercase()}"

// 마이페이지 목록과 메뉴별 화면을 하나의 그래프로 등록한다
// onNavigateToLogin: 로그아웃·회원탈퇴 뒤 메인을 벗어나 로그인 화면으로 간다 (app 에서 연결)
fun NavGraphBuilder.mypageGraph(
    navController: NavController,
    onNavigateToLogin: () -> Unit = {},
) {
    navigation(startDestination = MYPAGE_ROUTE, route = MYPAGE_GRAPH_ROUTE) {
        composable(MYPAGE_ROUTE) {
            MypageScreen(
                onMenuClick = { navController.navigate(mypageMenuRoute(it)) },
                onProfileEditClick = { navController.navigate(NICKNAME_EDIT_ROUTE) },
            )
        }
        composable(NICKNAME_EDIT_ROUTE) {
            NicknameEditRoute(onNavigateBack = navController::popBackStack)
        }
        composable(FAMILY_NAME_EDIT_ROUTE) {
            FamilyNameEditRoute(onNavigateBack = navController::popBackStack)
        }
        composable(TERMS_ROUTE) {
            TermsRoute(onNavigateBack = navController::popBackStack)
        }
        composable(OPEN_LICENSE_ROUTE) {
            OpenLicenseRoute(onNavigateBack = navController::popBackStack)
        }
        composable(WITHDRAW_ROUTE) {
            WithdrawRoute(
                onNavigateBack = navController::popBackStack,
                onNavigateToLogin = onNavigateToLogin,
            )
        }
        MypageMenu.entries.forEach { menu ->
            composable(mypageMenuRoute(menu)) {
                MypageMenuScreen(menu = menu, navController = navController, onNavigateToLogin = onNavigateToLogin)
            }
        }
    }
}

// 마이페이지 메뉴별 화면
@Composable
private fun MypageMenuScreen(
    menu: MypageMenu,
    navController: NavController,
    onNavigateToLogin: () -> Unit,
) {
    val onBackClick: () -> Unit = { navController.popBackStack() }
    when (menu) {
        MypageMenu.MY_PREFERENCE -> MyPreferenceScreen(onBackClick = onBackClick)
        MypageMenu.NOTIFICATION_SETTING -> NotificationSettingRoute(onNavigateBack = onBackClick)
        MypageMenu.FAMILY_SETTING -> FamilySettingScreen(
            onBackClick = onBackClick,
            onFamilyNameEditClick = { navController.navigate(FAMILY_NAME_EDIT_ROUTE) },
            onMyNicknameEditClick = { navController.navigate(NICKNAME_EDIT_ROUTE) },
        )
        MypageMenu.HOME_ADDRESS -> HomeAddressRoute(onNavigateBack = onBackClick)
        MypageMenu.INVITE_LINK -> InviteLinkRoute(onNavigateBack = onBackClick)
        MypageMenu.ACCOUNT_INFO -> AccountInfoRoute(
            onNavigateBack = onBackClick,
            onNavigateToTerms = { navController.navigate(TERMS_ROUTE) },
            onNavigateToOpenLicense = { navController.navigate(OPEN_LICENSE_ROUTE) },
            onNavigateToWithdraw = { navController.navigate(WITHDRAW_ROUTE) },
            onNavigateToLogin = onNavigateToLogin,
        )
        MypageMenu.SUGGESTION -> SuggestionRoute(onNavigateBack = onBackClick)
    }
}
