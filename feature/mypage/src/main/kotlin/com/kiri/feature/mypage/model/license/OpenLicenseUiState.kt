package com.kiri.feature.mypage.model.license

import com.kiri.feature.mypage.model.common.toLastUpdatedText
import java.time.LocalDate

data class OpenLicenseUiState(
    val licenses: List<OpenLicense> = OpenLicense.Libraries,
    // 세부 내용 팝업으로 보고 있는 라이선스 (null 이면 팝업이 닫혀 있다)
    val viewingLicense: OpenLicense? = null,
) {
    // TODO: 라이선스 목록의 최종 업데이트일이 따로 정해지면 교체 (현재는 이전 임시 약관과 같은 날짜)
    val lastUpdatedText: String
        get() = LAST_UPDATED.toLastUpdatedText()

    private companion object {
        val LAST_UPDATED: LocalDate = LocalDate.of(2026, 6, 26)
    }
}
