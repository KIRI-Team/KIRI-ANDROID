package com.kiri.feature.mypage.model.address

data class HomeAddress(
    val zipCode: String = "",
    val roadAddress: String = "",
    val detailAddress: String = "",
) {
    // 우편번호와 주소는 주소 검색으로 채운다. 상세주소는 없는 집도 있어서 비어 있어도 된다
    val isComplete: Boolean
        get() = zipCode.isNotBlank() && roadAddress.isNotBlank()

    // 주소 카드에 보여주는 한 줄 주소 (예: 강남구 영동대로 234, 710-1403호)
    val displayText: String
        get() = if (detailAddress.isBlank()) roadAddress else "$roadAddress, $detailAddress"
}
