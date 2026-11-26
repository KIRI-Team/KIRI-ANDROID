package com.kiri.feature.home.presentation

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AnswerEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun blankAnswerCannotSubmitAndTypedAnswerCanSubmit() {
        var submitted = ""
        compose.setContent {
            var draft by remember { mutableStateOf("") }
            AnswerEditor("내가 가장 좋아하는 음식은?", draft, false, null,
                { draft = it }, { submitted = draft }, {})
        }
        compose.onNodeWithText("답변 제출").assertIsNotEnabled()
        compose.onNodeWithContentDescription("오늘의 질문 답변").performTextInput("   ")
        compose.onNodeWithText("답변 제출").assertIsNotEnabled()
        compose.onNodeWithContentDescription("오늘의 질문 답변").performTextReplacement("가족과 먹는 불고기")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "kiri-answer-editor.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("답변 제출").performClick()
        compose.runOnIdle { assertEquals("가족과 먹는 불고기", submitted) }
    }

    @Test fun savingDisablesEditingAndSubmission() {
        compose.setContent {
            AnswerEditor("내가 가장 좋아하는 음식은?", "불고기", true, null, {}, {}, {})
        }
        compose.onNodeWithContentDescription("오늘의 질문 답변").assertIsNotEnabled()
        compose.onNodeWithText("저장 중…").assertIsNotEnabled()
        compose.onNodeWithContentDescription("뒤로").assertIsNotEnabled()
    }

    @Test fun answerInputIsLimitedToFiveHundredCharacters() {
        var draft = ""
        compose.setContent {
            var value by remember { mutableStateOf("") }
            AnswerEditor("오늘의 질문", value, false, null,
                { value = it; draft = it }, {}, {})
        }
        compose.onNodeWithContentDescription("오늘의 질문 답변").performTextInput("가".repeat(501))
        compose.runOnIdle { assertEquals(500, draft.length) }
    }
}
