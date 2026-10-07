package com.kiri.feature.onboarding.invite

import android.content.Context
import android.content.Intent
import com.kiri.domain.model.invite.Invite
import com.kiri.feature.onboarding.R

// 초대 링크를 카카오톡·메시지·링크 복사 등으로 보낼 수 있는 시스템 공유 시트 Intent
internal fun Context.createInviteShareIntent(invite: Invite): Intent {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_share_message, invite.url, invite.code))
        putExtra(Intent.EXTRA_TITLE, getString(R.string.invite_share_title))
    }
    return Intent.createChooser(send, null)
}
