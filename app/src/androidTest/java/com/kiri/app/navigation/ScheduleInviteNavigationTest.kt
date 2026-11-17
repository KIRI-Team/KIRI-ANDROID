package com.kiri.app.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.kiri.app.HiltTestActivity
import com.kiri.uikit.KiriTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// 카카오톡 일정 초대장으로 앱이 열리면 메인 화면이 그 일정 상세로 이동하는지 확인한다.
// 테스트용 일정 저장소(FakeCalendarScheduleModule)는 상세를 볼 수 없는 일정으로 답한다
@HiltAndroidTest
class ScheduleInviteNavigationTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var pendingScheduleInvite: PendingScheduleInvite

    @Before fun setUp() { hilt.inject() }

    @Test fun 일정_초대장으로_열리면_일정_상세로_이동하고_다시_열지_않는다() {
        pendingScheduleInvite.save(42)

        compose.setContent { KiriTheme { MainScreen() } }

        // 탭 첫 화면이 아닌 일정 상세로 가면 바텀 바가 사라진다
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("main_content").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag("floating_bottom_bar").fetchSemanticsNodes().isEmpty() &&
                pendingScheduleInvite.scheduleId.value == null
        }
        compose.waitForIdle()
        assertNull(pendingScheduleInvite.scheduleId.value)
        assertTrue("일정 상세가 홈 위에 떠야 한다", compose.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isEmpty())
    }
}
