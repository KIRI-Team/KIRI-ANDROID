package com.kiri.feature.record.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kiri.feature.detail.model.MemoryDraft
import com.kiri.uikit.KiriTheme
import org.junit.*
import org.junit.Assert.*

class MemoryEditorScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun requiredInputsGateRegistrationAndEditingRespectsTextLimit() {
        val draft = mutableStateOf(MemoryDraft())
        var registered = false
        lateinit var focusManager: androidx.compose.ui.focus.FocusManager
        compose.setContent {
            focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            KiriTheme { MemoryEditorScreen(draft.value, "끼리", { draft.value = it }, {}, { registered = true }) }
        }
        compose.onNodeWithTag("submit_memory").assertIsNotEnabled()
        compose.onNodeWithTag("memory_rating_4").performClick().assertIsSelected()
        compose.onNodeWithTag("memory_text").performScrollTo().performTextInput("가".repeat(501))
        assertEquals(500, draft.value.text.length)
        compose.onNodeWithTag("memory_repeat_false").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("submit_memory").assertIsNotEnabled()
        compose.runOnIdle { draft.value = draft.value.copy(photos = listOf("content://test/photo")) }
        compose.onNodeWithTag("submit_memory").assertIsEnabled()
        compose.runOnIdle { focusManager.clearFocus() }
        compose.onNodeWithTag("remove_memory_photo_0").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(draft.value.photos.isEmpty()) }
        compose.onNodeWithTag("submit_memory").assertIsNotEnabled()
        compose.runOnIdle { draft.value = draft.value.copy(photos = listOf("content://test/photo")) }
        compose.onNodeWithTag("submit_memory").assertIsEnabled().performClick()
        assertTrue(registered)
    }
}
