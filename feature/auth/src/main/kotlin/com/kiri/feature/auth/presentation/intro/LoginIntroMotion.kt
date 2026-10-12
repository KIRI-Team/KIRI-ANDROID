package com.kiri.feature.auth.presentation.intro

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing

// Figma "최종스플래시" 키프레임을 옮긴 로그인 첫 화면 애니메이션
// 시간은 ms, 위치·크기는 Figma 화면(390×844) 기준 dp. Figma 는 12초를 반복하지만 앱에서는 마지막 키프레임(약 3.8초)까지 한 번만 재생한다

// 한 구간의 시작 키프레임. easing 은 이 키프레임에서 다음 키프레임까지 적용한다 (CSS animation-timing-function 과 같다)
internal data class Key(val ms: Float, val value: Float, val easing: Easing = LinearEasing)

internal class Track(vararg keys: Key) {
    private val keys = keys.sortedBy { it.ms }

    fun valueAt(ms: Float): Float {
        if (ms <= keys.first().ms) return keys.first().value
        if (ms >= keys.last().ms) return keys.last().value
        val end = keys.indexOfFirst { it.ms > ms }
        val from = keys[end - 1]
        val to = keys[end]
        val fraction = (ms - from.ms) / (to.ms - from.ms)
        return from.value + (to.value - from.value) * from.easing.transform(fraction)
    }

    companion object {
        // 값만 바뀌고 모두 직선으로 잇는 트랙 (Figma 가 미리 샘플링한 바운스 등)
        fun linear(vararg points: Pair<Float, Float>) = Track(*points.map { Key(it.first, it.second) }.toTypedArray())
    }
}

// CSS linear(...) 처럼 같은 간격으로 샘플링한 값을 직선으로 이은 easing (Figma 스프링 곡선)
internal class SampledEasing(private val samples: FloatArray) : Easing {
    override fun transform(fraction: Float): Float {
        val position = fraction.coerceIn(0f, 1f) * (samples.size - 1)
        val index = position.toInt().coerceAtMost(samples.size - 2)
        val local = position - index
        return samples[index] + (samples[index + 1] - samples[index]) * local
    }
}

// 부드럽게 멈추는 스프링 (색·크기 변화)
internal val SpringSettle = SampledEasing(
    floatArrayOf(
        0f, 0.0216f, 0.0747f, 0.1458f, 0.2255f, 0.3076f, 0.3879f, 0.4638f, 0.5339f, 0.5974f, 0.6542f,
        0.7044f, 0.7484f, 0.7866f, 0.8196f, 0.8479f, 0.8722f, 0.8928f, 0.9103f, 0.9251f, 0.9375f,
        0.948f, 0.9568f, 0.9642f, 0.9703f, 0.9754f, 0.9797f, 0.9832f, 0.9862f, 0.9886f, 0.9906f,
        0.9923f, 0.9936f, 0.9948f, 0.9957f, 0.9965f, 0.9971f, 0.9976f, 0.9981f, 0.9984f, 0.9987f,
        0.9989f, 0.9991f, 0.9993f, 0.9994f, 0.9995f, 0.9996f, 0.9997f, 0.9997f, 0.9998f, 0.9998f,
    ),
)

// 살짝 지나쳤다 돌아오는 스프링 (조각 이동)
internal val SpringOvershoot = SampledEasing(
    floatArrayOf(
        0f, 0.0188f, 0.0679f, 0.1374f, 0.2195f, 0.308f, 0.3978f, 0.4856f, 0.5686f, 0.6452f, 0.7142f,
        0.7753f, 0.8283f, 0.8735f, 0.9113f, 0.9423f, 0.9671f, 0.9866f, 1.0014f, 1.0123f, 1.0198f,
        1.0247f, 1.0274f, 1.0283f, 1.0281f, 1.0268f, 1.025f, 1.0227f, 1.0202f, 1.0177f, 1.0152f,
        1.0128f, 1.0106f, 1.0085f, 1.0068f, 1.0052f, 1.0039f, 1.0028f, 1.0018f, 1.0011f, 1.0005f,
        1f, 0.9997f, 0.9995f, 0.9993f, 0.9992f, 0.9992f, 0.9992f, 0.9992f, 0.9993f, 0.9993f,
    ),
)

private val EaseInOut = CubicBezierEasing(0.5f, 0f, 0.5f, 1f)
private val PopIn = CubicBezierEasing(0.45f, 1.45f, 0.8f, 1f)
private val DropIn = CubicBezierEasing(0.05f, 0.4f, 0.5f, 1f)
private val EaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)

// Figma 12초 타임라인의 퍼센트를 ms 로 바꾼다
private fun pct(percent: Float) = percent * 120f

// Figma 화면 좌표의 가운데 점과 원본 벡터 크기(dp)로 놓는다
internal class IntroPiece(
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val translateX: Track,
    val translateY: Track,
    val scale: Track,
    // 0 이면 회색(#333333), 1 이면 초록(#9BEA51)
    val green: Track,
    val alpha: Track,
)

private fun piece(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    rotation: Float,
    translate: List<Triple<Float, Pair<Float, Float>, Easing>>,
    scale: Track,
    greenFrom: Float,
    greenTo: Float,
    fadeFrom: Float,
    fadeTo: Float,
) = IntroPiece(
    centerX = centerX,
    centerY = centerY,
    width = width,
    height = height,
    rotation = rotation,
    translateX = Track(*translate.map { Key(it.first, it.second.first, it.third) }.toTypedArray()),
    translateY = Track(*translate.map { Key(it.first, it.second.second, it.third) }.toTypedArray()),
    scale = scale,
    green = Track(Key(greenFrom, 0f, SpringSettle), Key(greenTo, 1f)),
    alpha = Track(Key(fadeFrom, 1f, EaseInOut), Key(fadeTo, 0f)),
)

internal object LoginIntroMotion {
    const val DESIGN_WIDTH = 390f
    const val DESIGN_HEIGHT = 844f

    // 마지막 키프레임(캐릭터가 자리를 잡는 시점)
    val END_MS = pct(31.96f)

    // 회색 글자 조각 4개가 초록으로 바뀌며 커지고 흩어진 뒤 사라진다 (Figma 9265:11150~11153)
    val pieces = listOf(
        piece(
            centerX = 145.525f, centerY = 267.474f, width = 105.918f, height = 112.538f, rotation = -3.54f,
            translate = listOf(
                Triple(0f, -13.365f to 88.764f, LinearEasing),
                Triple(pct(4.501f), -13.365f to 88.764f, LinearEasing),
                Triple(pct(9.395f), -13.371f to 88.771f, SpringOvershoot),
                Triple(pct(13.187f), -29.854f to 39.097f, LinearEasing),
                Triple(pct(14.323f), 34.716f to 38.357f, LinearEasing),
            ),
            scale = Track(
                Key(0f, 1f, SpringSettle), Key(pct(6.231f), 1.1f),
                Key(pct(9.395f), 1.1f, SpringSettle), Key(pct(13.187f), 1.2f),
            ),
            greenFrom = 0f, greenTo = pct(6.231f),
            fadeFrom = pct(13.547f), fadeTo = pct(13.93f),
        ),
        piece(
            centerX = 264.9065f, centerY = 285.669f, width = 44.1325f, height = 112.538f, rotation = 10.61f,
            translate = listOf(
                Triple(0f, -36.381f to 63.847f, LinearEasing),
                Triple(pct(4.501f), -36.381f to 63.847f, LinearEasing),
                Triple(pct(9.395f), -36.353f to 63.861f, SpringOvershoot),
                Triple(pct(13.187f), 21.374f to 29.99f, LinearEasing),
                Triple(pct(14.323f), -11.872f to 29.995f, LinearEasing),
            ),
            scale = Track(
                Key(0f, 1f), Key(pct(1.639f), 1f, SpringSettle),
                Key(pct(9.395f), 1.1f, EaseInOut), Key(pct(13.187f), 1.2f),
            ),
            greenFrom = pct(1.639f), greenTo = pct(8.524f),
            fadeFrom = pct(13.56f), fadeTo = pct(13.943f),
        ),
        piece(
            centerX = 173.855f, centerY = 420.922f, width = 105.918f, height = 113.641f, rotation = 11.34f,
            translate = listOf(
                Triple(0f, 35.316f to 61.6f, LinearEasing),
                Triple(pct(4.501f), 35.316f to 61.6f, LinearEasing),
                Triple(pct(9.395f), 35.34f to 61.6f, SpringOvershoot),
                Triple(pct(13.187f), -22.961f to 79.226f, LinearEasing),
                Triple(pct(14.323f), 2.6f to -18.44f, LinearEasing),
            ),
            scale = Track(
                Key(0f, 1f), Key(pct(3.307f), 1f, SpringSettle),
                Key(pct(9.395f), 1.1f, SpringOvershoot), Key(pct(13.187f), 1.2f),
            ),
            greenFrom = pct(3.307f), greenTo = pct(8.525f),
            fadeFrom = pct(13.56f), fadeTo = pct(13.943f),
        ),
        piece(
            centerX = 286.6165f, centerY = 420.915f, width = 44.1324f, height = 112.538f, rotation = -10.24f,
            translate = listOf(
                Triple(0f, 2.643f to 28.208f, LinearEasing),
                Triple(pct(5f), 2.643f to 28.208f, LinearEasing),
                Triple(pct(9.395f), 2.631f to 28.165f, SpringOvershoot),
                Triple(pct(13.187f), 31.811f to 70.679f, LinearEasing),
                Triple(pct(14.323f), -33.192f to -18.44f, LinearEasing),
            ),
            scale = Track(
                Key(0f, 1f), Key(pct(5f), 1f, SpringSettle),
                Key(pct(9.395f), 1.1f, SpringSettle), Key(pct(13.187f), 1.2f),
            ),
            greenFrom = pct(5f), greenTo = pct(8.524f),
            fadeFrom = pct(13.547f), fadeTo = pct(13.93f),
        ),
    )

    // KIRI 글자 로고: 조각이 사라질 때 나타나 크게 튀었다가 72% 크기로 줄어든다 (Figma 9265:11867)
    const val WORDMARK_CENTER_X = 195.5f
    const val WORDMARK_CENTER_Y = 361.62f
    val wordmarkAlpha = Track(Key(pct(13.187f), 0f, PopIn), Key(pct(14.138f), 1f))
    val wordmarkScale = Track.linear(
        0f to 1f, pct(13.311f) to 1f, pct(13.333f) to 1.007f, pct(14.167f) to 1.146f, pct(15f) to 1.184f,
        pct(15.833f) to 1.196f, pct(16.091f) to 1.198f, pct(16.667f) to 0.803f, pct(17.175f) to 0.728f,
        pct(17.5f) to 0.722f, pct(18.111f) to 0.72f,
    )

    // 캐릭터: 로고가 줄어들 때 위에서 떨어져 로고 위에 자리 잡는다 (Figma 9265:12279)
    const val CHARACTER_CENTER_X = 195f
    const val CHARACTER_CENTER_Y = 90.2f
    val characterTranslateY = Track.linear(
        0f to 105.099f, pct(16.082f) to 105.099f, pct(16.091f) to 105.097f, pct(16.667f) to 147.306f,
        pct(17.5f) to 177.819f, pct(18.333f) to 173.538f, pct(19.167f) to 175.855f, pct(20f) to 180.656f,
        pct(20.022f) to 180.797f, pct(20.833f) to 181.475f, pct(21.667f) to 179.545f, pct(22.5f) to 178.163f,
        pct(23.333f) to 178.336f, pct(24.167f) to 179.078f, pct(25f) to 179.391f, pct(25.833f) to 179.209f,
        pct(26.667f) to 178.965f, pct(27.5f) to 178.921f, pct(28.333f) to 179.012f, pct(29.167f) to 179.081f,
        pct(30f) to 179.075f, pct(30.833f) to 179.039f, pct(31.667f) to 179.023f, pct(31.96f) to 179.04f,
    )
    val characterAlpha = Track.linear(
        0f to 0f, pct(16.091f) to 0f, pct(16.667f) to 0.007f, pct(17.5f) to 0.127f, pct(18.333f) to 0.461f,
        pct(19.167f) to 0.776f, pct(19.415f) to 0.846f, pct(20f) to 0.994f, pct(20.022f) to 1f,
    )
    val characterRotation = Track(Key(pct(16.082f), -10.03f, DropIn), Key(pct(29.752f), 0f))
    val characterScale = Track(Key(pct(16.091f), 0.9f, DropIn), Key(pct(29.752f), 1f))

    // 로그인 버튼: 캐릭터가 거의 자리 잡을 때 서서히 나타난다 (Figma 9265:14366, 9265:14375)
    val buttonsAlpha = Track(Key(pct(24.415f), 0f, EaseIn), Key(pct(27.151f), 1f))
}
