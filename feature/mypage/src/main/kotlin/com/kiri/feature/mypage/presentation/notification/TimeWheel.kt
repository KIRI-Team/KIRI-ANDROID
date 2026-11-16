package com.kiri.feature.mypage.presentation.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.feature.mypage.R
import com.kiri.uikit.color.KiriColor
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private val BoxHeight = 58.dp
private val ItemHeight = 28.dp
private val SidePadding = 24.dp

// 위·아래로 끝없이 넘기려고 같은 값을 이만큼 반복해 둔다. 가운데에서 시작해 어느 쪽 끝에도 닿지 않는다
private const val LOOPS = 1_000

// 알림 시간 설정 칸 하나 (Figma Inputbox 100×58). 숫자만 위·아래로 넘어가고 단위(시·분)와 화살표는 제자리에 있다.
// 아이폰 알람처럼 가운데 숫자가 고른 값이고, 위·아래 숫자는 흐리게 반쯤 보인다.
// 손을 뗀 뒤 어떤 경우에도 두 숫자 사이에 멈추지 않는다:
//  - 밀어서 놓으면(느리게 놓아도) 가장 가까운 숫자로 붙는다 (snap fling)
//  - 넘어가는 중에 화면을 탭해 멈춘 것처럼 fling 없이 스크롤이 끝나도, 끝난 즉시 가장 가까운 숫자로 옮긴다
@Composable
internal fun TimeWheel(
    count: Int,
    initialValue: Int,
    unit: String,
    onValueChange: (Int) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = wheelStartIndex(initialValue, count))
    val itemPx = with(LocalDensity.current) { ItemHeight.roundToPx() }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    // 가운데에 가장 가까운 숫자. 넘기는 중에도 늘 정수 값이라 저장을 언제 눌러도 두 숫자 사이 값이 나오지 않는다
    val selected by remember(state, itemPx, count) {
        derivedStateOf {
            wheelValue(nearestWheelIndex(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset, itemPx), count)
        }
    }

    LaunchedEffect(state, itemPx, count) {
        snapshotFlow { selected }.distinctUntilChanged().collect { currentOnValueChange(it) }
    }
    LaunchedEffect(state, itemPx) {
        // 스크롤이 멈췄는데 숫자 사이에 걸쳐 있으면 맞춘다
        snapshotFlow { !state.isScrollInProgress && state.firstVisibleItemScrollOffset != 0 }
            .filter { stoppedBetween -> stoppedBetween }
            .collect { state.settle(itemPx) }
    }

    Box(
        modifier = modifier
            .size(width = 100.dp, height = BoxHeight)
            .clip(RoundedCornerShape(18.dp))
            .background(KiriColor.Bg.Secondary)
            .testTag(testTag)
            .semantics { contentDescription = "$selected$unit" },
    ) {
        val padding = (BoxHeight - ItemHeight) / 2
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = PaddingValues(vertical = padding),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(count * LOOPS) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ItemHeight)
                        .padding(start = SidePadding)
                        .graphicsLayer {
                            // 가운데에서 멀어질수록 흐리게
                            val center = state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset / itemPx.toFloat()
                            alpha = 1f - 0.75f * abs(index - center).coerceAtMost(1f)
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(text = twoDigits(wheelValue(index, count)), style = WheelTextStyle)
                }
            }
        }
        // 단위와 화살표는 숫자 자리를 투명한 "00" 으로 비워 둔 채 같은 줄에 고정한다. 터치는 받지 않아 아래 목록이 넘어간다
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SidePadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "00", style = WheelTextStyle, modifier = Modifier.alpha(0f))
            Spacer(Modifier.width(2.dp))
            Text(text = unit, style = WheelTextStyle)
            Spacer(Modifier.width(6.dp))
            Image(painter = painterResource(R.drawable.ic_mypage_time_updown), contentDescription = null)
        }
    }
}

// 스크롤이 두 숫자 사이에서 끝났으면 가장 가까운 숫자로 옮긴다.
// 옮기는 중에 사용자가 다시 잡으면 그 동작이 우선하고(이때는 손을 뗀 뒤 다시 불린다),
// 다른 이유로 옮기기가 끊겨 여전히 멈춘 채 걸쳐 있으면 다시 옮긴다
private suspend fun LazyListState.settle(itemPx: Int) {
    while (!isScrollInProgress && firstVisibleItemScrollOffset != 0) {
        val target = nearestWheelIndex(firstVisibleItemIndex, firstVisibleItemScrollOffset, itemPx)
        try {
            animateScrollToItem(target)
        } catch (e: CancellationException) {
            // 다른 스크롤에 밀려 취소된 경우만 넘어간다. 화면을 떠나 취소된 것이면 그대로 던진다
            currentCoroutineContext().ensureActive()
        }
    }
}

// 첫 숫자가 반 넘게 위로 지나갔으면 다음 숫자가 가운데에 더 가깝다
internal fun nearestWheelIndex(firstIndex: Int, firstOffsetPx: Int, itemPx: Int): Int =
    if (itemPx > 0 && firstOffsetPx * 2 >= itemPx) firstIndex + 1 else firstIndex

internal fun wheelValue(index: Int, count: Int): Int = index.mod(count)

// 반복해 둔 목록의 가운데쯤에서 value 가 가운데 오도록 시작한다
internal fun wheelStartIndex(value: Int, count: Int): Int = (LOOPS / 2) * count + value.mod(count)

private fun twoDigits(value: Int) = value.toString().padStart(2, '0')

private val WheelTextStyle = TextStyle(
    color = KiriColor.Text.Primary,
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 21.sp,
    letterSpacing = (-0.04).em,
)
