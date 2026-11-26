package com.kiri.feature.home.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.kiri.feature.onboarding.presentation.component.OnboardingRoleCard
import com.kiri.uikit.KiriTheme
import com.kiri.uikit.component.*
import com.kiri.uikit.color.KiriColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UiKitSpecTest {
    @get:Rule val compose = createComposeRule()

    @Test fun scheduleSelectionComponentsKeepTheirSlotsAndMemberStates() {
        var clicks = 0
        compose.setContent { KiriTheme {
            Column(Modifier.width(354.dp).background(KiriColor.Bg.Base)) {
                KiriFormSelectRow("일정 선택", "26.10.20(화) · 16:30", { clicks++ }, Modifier.testTag("form_select"))
                androidx.compose.foundation.layout.Row {
                    KiriFamilySelectionCard("마마", true, { clicks++ }, Modifier.width(150.dp).testTag("family_selected"))
                    KiriFamilySelectionCard("빠빠", false, { clicks++ }, Modifier.width(150.dp))
                }
                KiriParticipantTooltip(listOf(KiriParticipantStatus("mother", "마마", true, "수락"), KiriParticipantStatus("father", "빠빠", false, "재확인 필요")))
            }
        } }
        compose.onNodeWithTag("form_select").assertHeightIsEqualTo(85.dp).performClick()
        compose.onNodeWithTag("family_selected").assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(2, clicks) }
        capture("uikit-schedule-components")
    }

    @Test fun buttonVariantsKeepSizeAndDispatchClicks() {
        var clicks = 0
        compose.setContent {
            KiriTheme {
                Column(Modifier.width(354.dp).background(KiriColor.Bg.Base)) {
                    KiriButton({ clicks++ }, text = "기록", tone = KiriButtonTone.Neutral,
                        layout = KiriButtonLayout.Record, testTag = "record_button")
                    KiriButton({ clicks++ }, text = "초대", tone = KiriButtonTone.Neutral,
                        layout = KiriButtonLayout.Inline, testTag = "inline_button")
                    KiriButton({ clicks++ }, text = "비활성", enabled = false, testTag = "disabled_button")
                }
            }
        }
        compose.onNodeWithTag("record_button").assertHeightIsEqualTo(48.dp).performClick()
        compose.onNodeWithTag("inline_button").assertHeightIsEqualTo(30.dp).performClick()
        compose.onNodeWithTag("disabled_button").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(2, clicks) }
        capture("uikit-buttons")
    }

    @Test fun toastActionAndRoleSelectionRemainInteractive() {
        var actions = 0
        var selections = 0
        compose.setContent {
            KiriTheme {
                Column(Modifier.width(354.dp).background(KiriColor.Bg.Base)) {
                    KiriToast("버킷리스트에 저장되었습니다!", actionLabel = "바로가기", onAction = { actions++ })
                    OnboardingRoleCard("엄마", selected = true, onClick = { selections++ }, modifier = Modifier.testTag("role_card"))
                }
            }
        }
        compose.onNodeWithText("바로가기").performClick()
        compose.onNodeWithTag("role_card").assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(1, actions); assertEquals(1, selections) }
        capture("uikit-toast-role")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null))
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
