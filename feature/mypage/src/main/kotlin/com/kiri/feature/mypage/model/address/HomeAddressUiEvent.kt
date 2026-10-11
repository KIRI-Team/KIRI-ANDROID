package com.kiri.feature.mypage.model.address

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface HomeAddressUiEvent {
    data object EditClicked : HomeAddressUiEvent
    data object FindZipCodeClicked : HomeAddressUiEvent
    data class SearchQueryChanged(val query: String) : HomeAddressUiEvent
    data object SearchSubmitted : HomeAddressUiEvent

    // 주소 검색 결과에서 주소를 골랐다
    data class AddressSelected(val zipCode: String, val roadAddress: String) : HomeAddressUiEvent
    data class DetailAddressChanged(val detailAddress: String) : HomeAddressUiEvent
    data object RegisterClicked : HomeAddressUiEvent
    data object BackClicked : HomeAddressUiEvent
}
