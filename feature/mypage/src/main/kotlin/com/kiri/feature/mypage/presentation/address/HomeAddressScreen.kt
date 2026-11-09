package com.kiri.feature.mypage.presentation.address

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.mypage.HomeAddressStep
import com.kiri.feature.mypage.HomeAddressViewModel
import com.kiri.feature.mypage.model.address.HomeAddressUiEffect
import com.kiri.feature.mypage.model.address.HomeAddressUiEvent
import com.kiri.feature.mypage.model.address.HomeAddressUiState
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriTopBar
import com.kiri.uikit.component.KiriTopBarTextButton

@Composable
fun HomeAddressRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeAddressViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                HomeAddressUiEffect.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    HomeAddressScreen(modifier = modifier, state = state, onEvent = viewModel::onEvent)
}

// 우리집 주소 설정. 주소가 있으면 주소를 보여주고, 없으면 등록 화면을 보여준다
@Composable
fun HomeAddressScreen(
    state: HomeAddressUiState,
    onEvent: (HomeAddressUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onBackClick = { onEvent(HomeAddressUiEvent.BackClicked) }

    // 주소 보기·등록 화면에서는 비활성화해서 시스템 뒤로가기로 마이페이지로 돌아가게 한다
    BackHandler(
        enabled = state.currentStep == HomeAddressStep.EDIT || state.currentStep == HomeAddressStep.SEARCH,
        onBack = onBackClick,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .imePadding()
            .testTag("home_address_screen"),
    ) {
        val registerButton = @Composable {
            KiriTopBarTextButton(
                text = "등록하기",
                onClick = { onEvent(HomeAddressUiEvent.RegisterClicked) },
                enabled = state.canRegister,
            )
        }
        when (state.currentStep) {
            HomeAddressStep.SAVED -> KiriTopBar(onBackClick = onBackClick, title = TOP_BAR_TITLE)
            HomeAddressStep.REGISTER -> KiriTopBar(onBackClick = onBackClick, trailing = registerButton)
            // 수정 화면은 주소를 다 채웠을 때만 등록하기를 보여준다
            HomeAddressStep.EDIT -> KiriTopBar(
                onBackClick = onBackClick,
                title = TOP_BAR_TITLE,
                trailing = if (state.canRegister) registerButton else null,
            )
            HomeAddressStep.SEARCH -> KiriTopBar(onBackClick = onBackClick)
        }

        state.error?.let { androidx.compose.material3.Text(it, Modifier.testTag("home_address_error"), color = KiriColor.Status.Error) }
        if (state.saving) androidx.compose.material3.CircularProgressIndicator()
        when (state.currentStep) {
            HomeAddressStep.SAVED -> HomeAddressSavedContent(
                familyName = state.familyName,
                address = state.savedAddress?.displayText.orEmpty(),
                onEditClick = { onEvent(HomeAddressUiEvent.EditClicked) },
            )
            HomeAddressStep.REGISTER, HomeAddressStep.EDIT -> HomeAddressFormContent(
                isEdit = state.currentStep == HomeAddressStep.EDIT,
                form = state.form,
                onFindZipCodeClick = { onEvent(HomeAddressUiEvent.FindZipCodeClicked) },
                onDetailAddressChange = { onEvent(HomeAddressUiEvent.DetailAddressChanged(it)) },
            )
            HomeAddressStep.SEARCH -> PostcodePicker { onEvent(it) }
        }
    }
}

private const val TOP_BAR_TITLE = "우리 집 위치"

@Preview(showSystemUi = true)
@Composable
private fun HomeAddressScreenPreview() {
    HomeAddressScreen(state = HomeAddressUiState(familyName = "끼리네가족"), onEvent = {})
}
