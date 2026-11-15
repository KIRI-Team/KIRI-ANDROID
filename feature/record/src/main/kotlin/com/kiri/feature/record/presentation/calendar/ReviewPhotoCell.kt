package com.kiri.feature.record.presentation.calendar

import android.graphics.Bitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.pow

// 완료 후 리뷰가 있는 날짜 칸의 퍼즐 조각 모양 (Figma 10693:9281 "완료후 리뷰있는 약속", KIRI/Calendar Cell 추억기록날짜).
// 45 칸 기준으로 모서리 2, 오른쪽 가운데와 아래 가운데를 반지름 10 원으로 파낸다.
// 원의 중심은 칸 밖으로 오른쪽 3.5, 아래쪽 4 떨어져 있다 (Figma 에셋을 재서 얻은 값이라 칸 크기에 비례해 늘린다)
internal object ReviewPuzzleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val unit = size.width / 45f
        val base = Path().apply {
            addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(2f * unit)))
        }
        val notches = Path().apply {
            addOval(Rect(Offset(size.width + 3.5f * unit, size.height / 2f), 10f * unit))
            addOval(Rect(Offset(size.width / 2f, size.height + 4f * unit), 10f * unit))
        }
        return Outline.Generic(Path().apply { op(base, notches, PathOperation.Difference) })
    }
}

// 날짜 숫자가 놓이는 사진 가운데(가로·세로 40%)의 평균 상대 휘도(0~1).
// 칸은 사진을 가운데 기준으로 잘라 채우므로 사진 가운데가 숫자 뒤에 온다
internal fun Bitmap.centerLuminance(samples: Int = 8): Double {
    if (width == 0 || height == 0) return 0.0
    val left = width * 0.3
    val top = height * 0.3
    val pixels = IntArray(samples * samples) { i ->
        val x = (left + (i % samples + 0.5) * width * 0.4 / samples).toInt().coerceIn(0, width - 1)
        val y = (top + (i / samples + 0.5) * height * 0.4 / samples).toInt().coerceIn(0, height - 1)
        getPixel(x, y)
    }
    return averageLuminance(pixels)
}

// ARGB 픽셀들의 평균 상대 휘도 (WCAG 정의, 0~1)
internal fun averageLuminance(pixels: IntArray): Double {
    if (pixels.isEmpty()) return 0.0
    return pixels.sumOf { p ->
        0.2126 * channel(p shr 16) + 0.7152 * channel(p shr 8) + 0.0722 * channel(p)
    } / pixels.size
}

private fun channel(value: Int): Double {
    val c = (value and 0xFF) / 255.0
    return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
}

// 검정 숫자(Text.Inverse #1A1A1A)와 흰 숫자(Text.Primary #D9D9D8) 중 사진 위에서 대비가 더 큰 쪽이 검정이면 true
internal fun prefersDarkDayNumber(luminance: Double): Boolean =
    contrast(luminance, DARK_TEXT_LUMINANCE) > contrast(luminance, LIGHT_TEXT_LUMINANCE)

private fun contrast(a: Double, b: Double) = (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)

// #1A1A1A, #D9D9D8 의 상대 휘도
private const val DARK_TEXT_LUMINANCE = 0.0103
private const val LIGHT_TEXT_LUMINANCE = 0.6933
