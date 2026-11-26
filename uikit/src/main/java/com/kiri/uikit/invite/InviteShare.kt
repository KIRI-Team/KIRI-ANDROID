package com.kiri.uikit.invite

import android.content.Context
import android.content.Intent
import com.kiri.uikit.R

// 초대 링크를 카카오톡·메시지·링크 복사 등으로 보낼 수 있는 시스템 공유 시트 Intent
// 온보딩(가족 공간 생성)과 마이페이지(공용 초대 링크)가 함께 쓴다
fun Context.createInviteShareIntent(url: String, code: String): Intent {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_share_message, url, code))
        putExtra(Intent.EXTRA_TITLE, getString(R.string.invite_share_title))
    }
    return Intent.createChooser(send, null)
}
