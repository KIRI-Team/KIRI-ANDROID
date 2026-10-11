package com.kiri.feature.mypage.model.address

import com.kiri.feature.mypage.HomeAddressStep

data class HomeAddressUiState(
    val currentStep: HomeAddressStep = HomeAddressStep.REGISTER,
    val familyName: String = "",
    // 등록한 주소 (null이면 아직 없음)
    val savedAddress: HomeAddress? = null,
    // 등록·수정 화면에서 작성 중인 주소
    val form: HomeAddress = HomeAddress(),
    val searchQuery: String = "",
) {
    val canRegister: Boolean
        get() = form.isComplete
}
