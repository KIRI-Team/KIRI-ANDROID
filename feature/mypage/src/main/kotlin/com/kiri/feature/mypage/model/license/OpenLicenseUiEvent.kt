package com.kiri.feature.mypage.model.license

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface OpenLicenseUiEvent {
    data class LicenseClicked(val license: OpenLicense) : OpenLicenseUiEvent

    // 닫기 버튼, 바깥 탭, 뒤로가기로 팝업을 닫았다
    data object LicenseDialogClosed : OpenLicenseUiEvent
    data object BackClicked : OpenLicenseUiEvent
}
