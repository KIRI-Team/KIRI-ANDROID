package com.kiri.feature.mypage

import com.kiri.feature.mypage.model.license.OpenLicense
import com.kiri.feature.mypage.model.license.OpenLicenseUiEffect
import com.kiri.feature.mypage.model.license.OpenLicenseUiEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OpenLicenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModel = OpenLicenseViewModel()

    @Test
    fun 라이브러리마다_이름과_버전과_라이선스_전문이_있다() {
        val licenses = viewModel.uiState.value.licenses

        assertFalse(licenses.isEmpty())
        licenses.forEach {
            assertTrue(it.name.isNotBlank() && it.version.isNotBlank())
            assertTrue(it.notice.contains("Apache License, Version 2.0"))
            assertFalse(it.notice.contains("{holder}"))
        }
    }

    @Test
    fun 라이브러리를_누르면_팝업을_열고_닫으면_비운다() {
        val license = OpenLicense.Libraries.first()

        viewModel.onEvent(OpenLicenseUiEvent.LicenseClicked(license))
        assertEquals(license, viewModel.uiState.value.viewingLicense)

        viewModel.onEvent(OpenLicenseUiEvent.LicenseDialogClosed)
        assertNull(viewModel.uiState.value.viewingLicense)
    }

    @Test
    fun 뒤로가기를_누르면_돌아간다() = runTest {
        viewModel.onEvent(OpenLicenseUiEvent.BackClicked)

        assertEquals(OpenLicenseUiEffect.NavigateBack, viewModel.effect.first())
    }
}
