package com.kiri.feature.mypage.model.address

import com.kiri.feature.mypage.HomeAddressStep

data class HomeAddressUiState(
    val currentStep: HomeAddressStep = HomeAddressStep.REGISTER,
    // 등록한 주소를 불러오는 중. 불러오기 전에는 등록 화면을 보여주지 않는다
    val loading: Boolean = false,
    // 등록한 주소를 불러오지 못했을 때의 안내. 등록 화면에서 새로 등록할 수는 있다
    val loadError: String? = null,
    val familyName: String = "",
    // 등록한 주소 (null이면 아직 없음)
    val savedAddress: HomeAddress? = null,
    // 등록·수정 화면에서 작성 중인 주소
    val form: HomeAddress = HomeAddress(),
    val searchQuery: String = "",
    val saving: Boolean = false,
    val error: String? = null,
) {
    val canRegister: Boolean
        get() = form.isComplete && !saving && !loading
}
