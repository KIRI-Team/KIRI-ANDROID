package com.kiri.feature.detail.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.uikit.KiriTheme
import com.kiri.uikit.component.KiriPlaceCard
import java.io.File
import org.junit.Rule
import org.junit.Test

// Coil 로 사진을 그리고, 불러오지 못하면 기본 이미지를 보여주는지 확인한다 (실제 서버 주소는 쓰지 않는다)
class MemoryPhotoTest {
    @get:Rule val compose = createComposeRule()

    private fun photoFile(): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "memory-photo-test.png")
        file.outputStream().use { output ->
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.GREEN)
                compress(Bitmap.CompressFormat.PNG, 100, output)
                recycle()
            }
        }
        return android.net.Uri.fromFile(file).toString()
    }

    @Test fun 불러온_사진은_기본_아이콘_없이_보여준다() {
        compose.setContent { KiriTheme { MemoryPhoto(photoFile(), Modifier.size(100.dp), contentDescription = "가족 사진") } }

        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("사진을 표시할 수 없어요").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithContentDescription("가족 사진").assertExists()
    }

    @Test fun 불러올_수_없는_사진은_기본_아이콘을_보여준다() {
        compose.setContent { KiriTheme { MemoryPhoto("kiri-test://missing.png", Modifier.size(100.dp)) } }

        compose.waitForIdle()
        compose.onNodeWithContentDescription("사진을 표시할 수 없어요").assertExists()
    }

    @Test fun 장소_카드는_사진_주소가_비었거나_불러오지_못하면_기본_이미지를_쓴다() {
        compose.setContent {
            KiriTheme {
                androidx.compose.foundation.layout.Column {
                    KiriPlaceCard("빈 주소", "장소", "설명", rating = null, imageUrl = " ")
                    KiriPlaceCard("없는 사진", "장소", "설명", rating = null, imageUrl = "kiri-test://missing.png")
                }
            }
        }

        compose.waitForIdle()
        // 빈 주소면 기존처럼 기본 이미지(제목이 설명)를 그린다
        compose.onNodeWithContentDescription("빈 주소").assertExists()
        // 불러오지 못한 카드도 카드 자체는 그대로 남는다
        compose.onNodeWithText("없는 사진").assertIsDisplayed()
    }
}
