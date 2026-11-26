package com.kiri.feature.mypage.presentation.account

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.mypage.AccountInfoViewModel
import com.kiri.feature.mypage.R
import com.kiri.feature.mypage.model.account.AccountInfoUiEffect
import com.kiri.feature.mypage.model.account.AccountInfoUiEvent
import com.kiri.feature.mypage.model.account.AccountInfoUiState
import com.kiri.feature.mypage.model.common.MypageMenu
import com.kiri.feature.mypage.presentation.common.MypageDialog
import com.kiri.feature.mypage.presentation.common.MypageDialogButton
import com.kiri.feature.mypage.presentation.common.MypageDialogButtonTone
import com.kiri.feature.mypage.presentation.common.MypageMenuCard
import com.kiri.feature.mypage.presentation.common.MypageMenuRow
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriTopBar

@Composable
fun AccountInfoRoute(
    onNavigateBack: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToOpenLicense: () -> Unit,
    onNavigateToWithdraw: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val currentOnNavigateToTerms by rememberUpdatedState(onNavigateToTerms)
    val currentOnNavigateToOpenLicense by rememberUpdatedState(onNavigateToOpenLicense)
    val currentOnNavigateToWithdraw by rememberUpdatedState(onNavigateToWithdraw)
    val currentOnNavigateToLogin by rememberUpdatedState(onNavigateToLogin)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AccountInfoUiEffect.NavigateToTerms -> currentOnNavigateToTerms()
                AccountInfoUiEffect.NavigateToOpenLicense -> currentOnNavigateToOpenLicense()
                AccountInfoUiEffect.NavigateToWithdraw -> currentOnNavigateToWithdraw()
                AccountInfoUiEffect.NavigateToLogin -> currentOnNavigateToLogin()
                AccountInfoUiEffect.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    AccountInfoScreen(modifier = modifier, state = state, onEvent = viewModel::onEvent)
}

// 계정 정보. 약관·라이선스·앱 버전과 로그아웃·회원탈퇴 메뉴
@Composable
fun AccountInfoScreen(
    state: AccountInfoUiState,
    onEvent: (AccountInfoUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    appVersion: String = rememberAppVersion(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .testTag("account_info_screen"),
    ) {
        KiriTopBar(onBackClick = { onEvent(AccountInfoUiEvent.BackClicked) }, title = MypageMenu.ACCOUNT_INFO.title)
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(31.dp))
            MypageMenuCard {
                MypageMenuRow(
                    title = "이용 약관",
                    testTag = "account_info_terms",
                    onClick = { onEvent(AccountInfoUiEvent.TermsClicked) },
                )
                MypageMenuRow(
                    title = "오픈 라이선스",
                    testTag = "account_info_open_license",
                    onClick = { onEvent(AccountInfoUiEvent.OpenLicenseClicked) },
                )
                MypageMenuRow(title = "앱 버전", testTag = "account_info_app_version", onClick = null, trailingText = appVersion)
            }
            Spacer(Modifier.height(36.dp))
            MypageMenuCard {
                MypageMenuRow(
                    title = "로그아웃",
                    testTag = "account_info_logout",
                    onClick = { onEvent(AccountInfoUiEvent.LogoutClicked) },
                )
                MypageMenuRow(
                    title = "회원탈퇴",
                    testTag = "account_info_withdraw",
                    onClick = { onEvent(AccountInfoUiEvent.WithdrawClicked) },
                )
            }
        }
    }
    if (state.isLogoutDialogVisible) {
        LogoutDialog(
            onCancel = { onEvent(AccountInfoUiEvent.LogoutCanceled) },
            onConfirm = { onEvent(AccountInfoUiEvent.LogoutConfirmed) },
        )
    }
}

@Composable
private fun LogoutDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    MypageDialog(
        title = "정말로 로그아웃 하시겠어요?",
        subtitle = "다시 로그인하면 우리 가족의 기록을 이어갈 수 있어요.",
        onDismiss = onCancel,
        testTag = "logout_dialog",
        buttons = {
            MypageDialogButton(
                text = "취소",
                onClick = onCancel,
                tone = MypageDialogButtonTone.Neutral,
                testTag = "logout_dialog_cancel",
            )
            MypageDialogButton(
                text = "확인",
                onClick = onConfirm,
                tone = MypageDialogButtonTone.Accent,
                testTag = "logout_dialog_confirm",
            )
        },
    ) {
        Image(painter = painterResource(R.drawable.img_mypage_logout), contentDescription = null)
    }
}

// 설치된 앱의 versionName (app 모듈의 versionName을 따른다)
@Composable
private fun rememberAppVersion(): String {
    val context = LocalContext.current
    return remember(context) { context.appVersionName() }
}

private fun Context.appVersionName(): String {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    return packageInfo.versionName.orEmpty()
}

@Preview(showSystemUi = true)
@Composable
private fun AccountInfoScreenPreview() {
    AccountInfoScreen(state = AccountInfoUiState(), onEvent = {}, appVersion = "1.0.0")
}
