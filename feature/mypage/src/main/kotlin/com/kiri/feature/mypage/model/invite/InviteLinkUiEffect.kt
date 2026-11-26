package com.kiri.feature.mypage.model.invite

// ViewModel → 화면 으로 전달하는 일회성 동작 (복사, 공유, 화면 이동 등)
sealed interface InviteLinkUiEffect {
    // label: 클립보드에 남길 이름 (예: 가족코드)
    data class CopyToClipboard(val label: String, val text: String) : InviteLinkUiEffect

    // 시스템 공유 시트로 초대 링크를 공유한다
    data class ShareInvite(val url: String, val code: String) : InviteLinkUiEffect

    // 초대 코드를 발급하지 못했다
    data object ShowInviteError : InviteLinkUiEffect

    // 지금 초대를 불러오지 못했다 (네트워크, 가족 공간 정보 없음 등)
    data object ShowLoadError : InviteLinkUiEffect

    // 마이페이지로 돌아간다
    data object NavigateBack : InviteLinkUiEffect
}
