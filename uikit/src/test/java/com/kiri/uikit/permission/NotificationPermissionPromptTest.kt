package com.kiri.uikit.permission

import com.kiri.uikit.permission.NotificationPermissionMoment.HOME
import com.kiri.uikit.permission.NotificationPermissionMoment.TERMS
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 알림 권한은 약관 화면에서 처음, 거절했으면 홈에서 한 번 더 묻는다
class NotificationPermissionPromptTest {

    private val none = NotificationPermissionRecord()

    private fun ask(
        moment: NotificationPermissionMoment,
        record: NotificationPermissionRecord = none,
        granted: Boolean = false,
        sdkInt: Int = 33,
        askedThisLaunch: Boolean = false,
    ) = shouldAskNotificationPermission(sdkInt, granted, moment, record, askedThisLaunch)

    @Test
    fun Android_13_미만이거나_이미_허용했으면_묻지_않는다() {
        assertFalse(ask(TERMS, sdkInt = 32))
        assertFalse(ask(HOME, sdkInt = 32))
        assertFalse(ask(TERMS, granted = true))
        assertFalse(ask(HOME, granted = true))
    }

    @Test
    fun 약관_화면에서는_처음_한_번만_묻는다() {
        assertTrue(ask(TERMS))
        assertFalse(ask(TERMS, NotificationPermissionRecord(askedOnTerms = true)))
        // 홈에서 이미 물은 사람(약관에 먼저 동의해 둔 기존 회원)이 다시 로그인해도 묻지 않는다
        assertFalse(ask(TERMS, NotificationPermissionRecord(askedOnHome = true)))
    }

    @Test
    fun 약관에서_거절했으면_다음_실행의_홈에서_한_번_더_묻는다() {
        val askedOnTerms = NotificationPermissionRecord(askedOnTerms = true)

        // 약관 화면에서 방금 거절하고 온보딩을 거쳐 홈에 왔을 때는 바로 다시 묻지 않는다
        assertFalse(ask(HOME, askedOnTerms, askedThisLaunch = true))
        assertTrue(ask(HOME, askedOnTerms))
        assertFalse(ask(HOME, NotificationPermissionRecord(askedOnTerms = true, askedOnHome = true)))
    }

    @Test
    fun 약관_화면을_거치지_않은_회원은_홈에서_처음_묻는다() {
        assertTrue(ask(HOME))
    }
}
