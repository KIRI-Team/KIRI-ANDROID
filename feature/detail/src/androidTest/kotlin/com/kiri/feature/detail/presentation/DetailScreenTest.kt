package com.kiri.feature.detail.presentation

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.domain.detail.Condition
import com.kiri.feature.detail.model.*
import com.kiri.uikit.KiriTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DetailScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun completedDiscoveryCanResaveAndThenShowsSaved() {
        val state = mutableStateOf(MissionPreviewUiState(loading = false,
            preview = com.kiri.domain.detail.MissionPreview(figmaDetailSample().copy(completed = true)),
            previouslyCompleted = true))
        var saves = 0
        compose.setContent {
            KiriTheme {
                MissionPreviewScreen(state.value, {}, {}, {}, onSave = {
                    saves++
                    state.value = state.value.copy(saved = true)
                })
            }
        }
        compose.onNodeWithTag("preview_save").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("다시 버킷에 저장").assertIsDisplayed()
        capture("preview-resave")
        compose.onNodeWithTag("preview_save").performClick()
        assertEquals(1, saves)
        compose.onNodeWithTag("preview_save").assertIsNotEnabled()
        compose.onNodeWithText("저장됨").assertIsDisplayed()
        capture("preview-saved")
    }

    @Test
    fun detailShowsDataScrollsAndOpensCustom() {
        var opened = false
        compose.setContent {
            KiriTheme {
                MissionDetailScreen(
                    DetailUiState(false, figmaDetailSample()),
                    { if (it == DetailUiEvent.CustomClicked) opened = true },
                )
            }
        }
        compose.onNodeWithText("한강 뚜벅뚜벅 축제 가보기").assertIsDisplayed()
        compose.onNodeWithTag("open_custom").performClick()
        assertTrue(opened)
        capture("detail-top")
        compose.onNodeWithText("한강 뚜벅뚜벅 축제 가보기 근처 놀거리추천").performScrollTo().assertIsDisplayed()
        capture("detail-bottom")
    }

    @Test
    fun customSupportsMultipleSelectionAndApply() {
        val state =
            mutableStateOf(
                DetailUiState(
                    false,
                    figmaDetailSample().copy(expectedSteps = 8000, title = "한강 뚜벅뚜벅 축제 방문"),
                )
            )
        var applied: Set<Condition>? = null
        compose.setContent {
            KiriTheme {
                ConditionCustomScreen(
                    state.value,
                    onEvent = { event ->
                        when (event) {
                            is DetailUiEvent.ConditionToggled -> {
                                val c = event.condition
                                val old = state.value
                                state.value =
                                    old.copy(
                                        draft = if (c in old.draft) old.draft - c else old.draft + c
                                    )
                            }
                            DetailUiEvent.ApplyClicked -> applied = state.value.draft
                            else -> Unit
                        }
                    },
                )
            }
        }
        capture("custom-default")
        compose.onNodeWithTag("condition_LESS_WALKING").performClick().assertIsOn()
        compose.onNodeWithTag("condition_MORE_REST").performClick().assertIsOn()
        capture("custom-selected")
        compose.onNodeWithTag("condition_LESS_WALKING").performClick().assertIsOff()
        compose.onNodeWithTag("apply_custom").performClick()
        assertEquals(setOf(Condition.MORE_REST), applied)
    }

    @Test
    fun completedRecordActionOpensEditor() {
        var navigated = false
        compose.setContent {
            KiriTheme {
                MissionDetailScreen(
                    DetailUiState(false, figmaDetailSample(true)),
                    { if (it == DetailUiEvent.RecordClicked) navigated = true },
                )
            }
        }
        compose
            .onNodeWithTag("open_memory_editor")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        assertTrue(navigated)
        capture("detail-completed")
    }

    @Test
    fun savingDisablesAllSelectionAndApply() {
        compose.setContent {
            KiriTheme {
                ConditionCustomScreen(DetailUiState(false, figmaDetailSample(), saving = true), {})
            }
        }
        compose.onNodeWithTag("apply_custom").assertIsNotEnabled()
        Condition.entries.forEach {
            compose.onNodeWithTag("condition_${it.name}").assertIsNotEnabled()
        }
    }

    @Test
    fun familyMemoryTabShowsEmptyAndFutureActivitiesCannotRecord() {
        val state =
            mutableStateOf(
                DetailUiState(
                    false,
                    figmaDetailSample().copy(date = java.time.LocalDate.now().plusDays(3)),
                )
            )
        compose.setContent {
            KiriTheme {
                MissionDetailScreen(
                    state.value,
                    { event ->
                        if (event is DetailUiEvent.MemoriesSelected)
                            state.value = state.value.copy(memoriesSelected = event.selected)
                    },
                )
            }
        }
        compose
            .onNodeWithTag("mission_memory_tab")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        compose.onNodeWithTag("family_memories_empty").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("아직 기록이 없어요").assertIsDisplayed()
        compose.onNodeWithTag("open_memory_editor").performScrollTo().assertIsNotEnabled()
        compose
            .onNodeWithTag("mission_activity_tab")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        compose.onNodeWithTag("family_memories_empty").assertDoesNotExist()
    }

    @Test
    fun submittedMemoryShowsOnlyOwnCardAndEditAction() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image =
            android.graphics.BitmapFactory.decodeResource(
                context.resources,
                com.kiri.uikit.R.drawable.kiri_place_card_image,
            )
        val photo = File(context.cacheDir, "memory-preview-photo.png")
        photo.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        var edited = false
        val memory =
            MemoryDraft(
                4,
                listOf(android.net.Uri.fromFile(photo).toString()),
                "가족과 함께 즐거운 하루를 보냈어요.",
                true,
            )
        compose.setContent {
            KiriTheme {
                MissionDetailScreen(
                    DetailUiState(
                        false,
                        figmaDetailSample(true),
                        memoriesSelected = true,
                        submittedMemory = memory,
                    ),
                    { if (it == DetailUiEvent.RecordClicked) edited = true },
                )
            }
        }
        compose.onNodeWithTag("local_memory_preview").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithTag("local_memory_preview").assertCountEquals(1)
        capture("memory-completed")
        compose.onNodeWithTag("edit_memory").performClick()
        assertTrue(edited)
        compose.onNodeWithTag("family_memories_empty").assertDoesNotExist()
    }

    @Test
    fun loadFailureOffersRetryAndBack() {
        var retried = false
        var backed = false
        compose.setContent {
            KiriTheme {
                MissionDetailScreen(
                    DetailUiState(loading = false, error = "불러오기 실패"),
                    {
                        if (it == DetailUiEvent.BackClicked) backed = true
                        if (it == DetailUiEvent.RetryClicked) retried = true
                    },
                )
            }
        }
        compose.onNodeWithText("다시 시도").performClick()
        assertTrue(retried)
        compose.onNodeWithContentDescription("뒤로").performClick()
        assertTrue(backed)
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output =
            InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
                ?: requireNotNull(context.getExternalFilesDir(null))
        output.mkdirs()
        val file = File(output, "$name.png")
        file.outputStream().use {
            compose
                .onRoot()
                .captureToImage()
                .asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
