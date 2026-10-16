package com.kiri.feature.place.presentation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.kiri.domain.place.PlaceFilters
import com.kiri.uikit.KiriTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class PlaceFilterSheetTest {
    @get:Rule val compose = createComposeRule()
    @Composable private fun Harness() {
        var open by rememberSaveable { mutableStateOf(true) }
        var applied by rememberSaveable(stateSaver = PlaceFiltersSaver) { mutableStateOf(PlaceFilters()) }
        TextButton(onClick = { open = true }) { Text("필터 열기") }
        if (open) PlaceFilterSheet(applied, { open = false }) { applied = it; open = false }
    }
    @Test fun multiRegionApplyDismissAndResetAreTransactional() {
        compose.setContent { KiriTheme { Harness() } }
        compose.onNodeWithTag("filter_region_서울").performClick()
        compose.onNodeWithTag("filter_region_인천").performClick()
        compose.onNodeWithTag("filter_apply").performClick()
        compose.onNodeWithText("필터 열기").performClick()
        compose.onNodeWithTag("filter_region_서울").assertIsSelected()
        compose.onNodeWithTag("filter_region_인천").assertIsSelected()
        compose.onNodeWithTag("filter_region_경기").performClick()
        compose.onNodeWithTag("place_filter_close").performClick()
        compose.onNodeWithText("필터 열기").performClick()
        compose.onNodeWithTag("filter_region_경기").assertIsNotSelected()
        compose.onNodeWithTag("filter_reset").performClick()
        compose.onNodeWithTag("filter_region_전체").assertIsSelected()
    }
    @Test fun datePeopleAndTimeApplyTogetherAndCanBeCleared() {
        compose.setContent { KiriTheme { Harness() } }
        compose.onNodeWithTag("filter_date").performClick()
        val date = LocalDate.now().withDayOfMonth(15)
        compose.onNodeWithTag("filter_day_$date").performClick()
        compose.onNodeWithTag("filter_time_570").performClick()
        compose.onNodeWithTag("filter_people_3").performScrollTo().performClick()
        compose.onNodeWithTag("filter_apply").performClick()
        compose.onNodeWithTag("filter_date_clear").assertIsDisplayed().performClick()
        compose.onNodeWithText("날짜·인원·시간").assertIsDisplayed()
        compose.onNodeWithTag("filter_date").performClick()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("날짜·인원·시간").assertIsDisplayed()
    }
    @Test fun openDraftSurvivesRecreationAndSectionAnchorsReachPrice() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { KiriTheme { Harness() } }
        compose.onNodeWithTag("filter_region_경기").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("filter_region_경기").assertIsSelected()
        compose.onNodeWithTag("filter_section_3").performClick()
        compose.onNodeWithTag("filter_price_min").assertIsDisplayed()
        compose.onNodeWithTag("filter_price_max").assertTextEquals("1,000,000원")
        compose.onNodeWithTag("filter_apply").assertIsDisplayed()
    }
}
