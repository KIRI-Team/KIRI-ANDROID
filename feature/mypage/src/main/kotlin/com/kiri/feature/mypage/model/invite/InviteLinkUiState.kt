package com.kiri.feature.mypage.model.invite

import com.kiri.domain.model.invite.Invite

data class InviteLinkUiState(
    // 지금 쓸 수 있는 초대 (아직 불러오지 못했거나 발급하지 못했으면 null)
    val invite: Invite? = null,
    // 새 초대를 발급하는 동안 로딩 화면을 보여준다
    val isIssuing: Boolean = false,
    // 리셋하면서 이전 초대는 지웠지만 새 초대를 받지 못했다 (다시 리셋해야 초대를 보낼 수 있다)
    val isReissueFailed: Boolean = false,
) {
    val code: String
        get() = invite?.code.orEmpty()

    // 링크는 코드 부분을 가려서 보여준다 (예: kiri.example.com/invite/••••••)
    val maskedLink: String
        get() = invite?.let { it.url.substringAfter("://").removeSuffix(it.code) + MASK.repeat(it.code.length) }.orEmpty()

    // 예: 10월 12일까지 사용가능
    val expiresText: String?
        get() = invite?.expiresAt?.let { "${it.monthValue}월 ${it.dayOfMonth}일까지 사용가능" }

    val canShare: Boolean
        get() = invite != null && !isIssuing

    private companion object {
        const val MASK = "•"
    }
}
