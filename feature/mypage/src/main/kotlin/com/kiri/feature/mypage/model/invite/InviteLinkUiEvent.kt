package com.kiri.feature.mypage.model.invite

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface InviteLinkUiEvent {
    data object CodeCopyClicked : InviteLinkUiEvent
    data object LinkCopyClicked : InviteLinkUiEvent

    // 리셋하기: 새 초대를 발급한다
    data object ResetClicked : InviteLinkUiEvent
    data object ShareClicked : InviteLinkUiEvent

    // 공유 시트를 닫고 돌아왔다
    data object InviteShared : InviteLinkUiEvent
    data object BackClicked : InviteLinkUiEvent
}
