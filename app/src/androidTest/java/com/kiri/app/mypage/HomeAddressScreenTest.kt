package com.kiri.app.mypage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.feature.mypage.HomeAddressStep
import com.kiri.feature.mypage.model.address.HomeAddress
import com.kiri.feature.mypage.model.address.HomeAddressUiEvent
import com.kiri.feature.mypage.model.address.HomeAddressUiState
import com.kiri.feature.mypage.presentation.address.HomeAddressScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeAddressScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<HomeAddressUiEvent>()

    private val selectedAddress = HomeAddress(zipCode = "06134", roadAddress = "서울 서초구 강남대로 143")

    private fun setContent(state: HomeAddressUiState) {
        composeTestRule.setContent {
            KiriTheme { HomeAddressScreen(state = state, onEvent = { events += it }) }
        }
    }

    @Test
    fun 주소가_없으면_등록_화면이_보이고_등록하기가_비활성화된다() {
        setContent(HomeAddressUiState())

        composeTestRule.onNodeWithText("아직 작성된 주소가 없어요!").assertIsDisplayed()
        composeTestRule.onNodeWithText("우리 집 주소 작성하기").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kiri_top_bar_text_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("home_address_detail").assertIsNotEnabled()
    }

    @Test
    fun 주소를_고르면_등록하기와_상세주소가_활성화되고_이벤트를_전달한다() {
        setContent(HomeAddressUiState(form = selectedAddress))

        composeTestRule.onNodeWithText("우리 집 주소").assertIsDisplayed()
        composeTestRule.onNodeWithText("서울 서초구 강남대로 143").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_address_detail").assertIsEnabled().performTextInput("810-603")
        composeTestRule.onNodeWithTag("kiri_top_bar_text_button").assertIsEnabled().performClick()

        assertEquals(
            listOf(HomeAddressUiEvent.DetailAddressChanged("810-603"), HomeAddressUiEvent.RegisterClicked),
            events,
        )
    }

    @Test
    fun 우편번호_찾기를_누르면_이벤트를_전달한다() {
        setContent(HomeAddressUiState())

        composeTestRule.onNodeWithText("우편번호 찾기").performClick()

        assertEquals(listOf(HomeAddressUiEvent.FindZipCodeClicked), events)
    }

    @Test
    fun 등록한_주소와_가족_이름이_보이고_수정하기를_누르면_이벤트를_전달한다() {
        setContent(
            HomeAddressUiState(
                currentStep = HomeAddressStep.SAVED,
                familyName = "끼리네가족",
                savedAddress = HomeAddress("06134", "강남구 영동대로 234", "710-1403호"),
            ),
        )

        composeTestRule.onNodeWithText("우리 집 위치").assertIsDisplayed()
        composeTestRule.onNodeWithText("끼리네가족님의 공간").assertIsDisplayed()
        composeTestRule.onNodeWithText("강남구 영동대로 234, 710-1403호").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_address_edit").performClick()

        assertEquals(listOf(HomeAddressUiEvent.EditClicked), events)
    }

    @Test
    fun 수정_화면은_주소를_채우기_전까지_등록하기를_보여주지_않는다() {
        setContent(HomeAddressUiState(currentStep = HomeAddressStep.EDIT))

        composeTestRule.onNodeWithText("주소를 수정해주세요").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kiri_top_bar_text_button").assertDoesNotExist()
    }

    @Test
    fun 수정_화면에서_주소를_채우면_등록하기가_보인다() {
        setContent(HomeAddressUiState(currentStep = HomeAddressStep.EDIT, form = selectedAddress))

        composeTestRule.onNodeWithTag("kiri_top_bar_text_button").assertIsEnabled()
    }

    @Test
    fun 검색한_주소와_법정동코드를_선택하고_뒤로가기_이벤트를_전달한다() {
        val selected = HomeAddressUiEvent.AddressSelected("06134", "서울 서초구 강남대로 143", "1165010800")
        composeTestRule.setContent {
            KiriTheme {
                HomeAddressScreen(HomeAddressUiState(currentStep = HomeAddressStep.SEARCH), { events += it },
                    addressSearch = { choose ->
                        androidx.compose.material3.Button({ choose(selected) }) { androidx.compose.material3.Text("주소 선택") }
                    })
            }
        }
        composeTestRule.onNodeWithText("주소 선택").performClick()
        composeTestRule.onNodeWithTag("kiri_back_button").performClick()
        assertEquals(listOf(selected, HomeAddressUiEvent.BackClicked), events)
    }
}
