package com.kiri.feature.home.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.kiri.feature.home.R
import com.kiri.feature.home.model.HomeTutorialStep
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography
import kotlin.math.roundToInt

internal enum class HomeTutorialTarget { ANSWERS, INVITE, CARD, FLIP, RECOMMENDATIONS }

private data class TutorialAnnotation(val target: HomeTutorialTarget, val message: String, val image: Int, val endAligned: Boolean = false)

/** Bounds come from the live home layout, never from the Figma screen coordinates. */
@Composable
internal fun HomeTutorialOverlay(
    step: HomeTutorialStep,
    anchors: Map<HomeTutorialTarget, Rect>,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    val annotations = when (step) {
        HomeTutorialStep.INITIAL -> listOf(
            TutorialAnnotation(HomeTutorialTarget.ANSWERS, "우리 가족들의\n답변을 모아봐요!", R.drawable.tutorial_header_highlight),
            TutorialAnnotation(HomeTutorialTarget.INVITE, "가족을 초대해서\n방을 만들어요!", R.drawable.tutorial_header_highlight, true),
            TutorialAnnotation(HomeTutorialTarget.CARD, "질문은 매일 하나씩 발행돼요!", R.drawable.tutorial_card_highlight, true),
        )
        HomeTutorialStep.MISSION -> listOf(TutorialAnnotation(HomeTutorialTarget.CARD, "우리 가족의 답변을 바탕으로\n매주 한 개의 미션이 발행돼요!", R.drawable.tutorial_card_highlight, true))
        HomeTutorialStep.RECOMMENDATIONS -> listOf(TutorialAnnotation(HomeTutorialTarget.RECOMMENDATIONS, "끼리가 큐레이팅 한 미션 목록!", R.drawable.tutorial_recommendation_highlight))
    }.filter { it.target in anchors }
    Box(Modifier.fillMaxSize().testTag("home_tutorial_${step.name}")
        .semantics { paneTitle = "홈 사용 안내 ${step.ordinal + 1}단계" }
        .background(KiriColor.Bg.Base.copy(alpha = .75f)).clickable(onClick = onNext)) {
        Layout(content = {
            annotations.forEach { annotation ->
                Image(painterResource(annotation.image), null, Modifier.testTag("tutorial_highlight_${annotation.target.name}"), contentScale = ContentScale.FillBounds)
                Text(annotation.message, color = KiriColor.Text.Primary, style = KiriDetailTypography.Title, textAlign = if (annotation.endAligned) TextAlign.End else TextAlign.Start)
            }
            if (step == HomeTutorialStep.MISSION) {
                Text("우상단 플립 버튼을 눌러\n질문카드도 확인할 수 있어요!", color = KiriColor.Text.Primary, style = KiriDetailTypography.Title, textAlign = TextAlign.End)
                Image(painterResource(R.drawable.tutorial_flip_spotlight), null, contentScale = ContentScale.FillBounds)
                Image(painterResource(R.drawable.tutorial_flip_line), null, contentScale = ContentScale.FillBounds)
            }
        }, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
            val margin = 18.dp.roundToPx()
            val padding = 4.dp.roundToPx()
            val footer = 76.dp.roundToPx()
            val maxLabelBottom = (constraints.maxHeight - footer).coerceAtLeast(0)
            val placements = mutableListOf<Triple<androidx.compose.ui.layout.Placeable, Int, Int>>()
            annotations.forEachIndexed { index, annotation ->
                val rect = anchors.getValue(annotation.target)
                val header = annotation.target == HomeTutorialTarget.ANSWERS || annotation.target == HomeTutorialTarget.INVITE
                val x = (rect.left.roundToInt() - padding).coerceAtLeast(0)
                val y = (rect.top.roundToInt() - padding).coerceAtLeast(0)
                val width = (rect.width.roundToInt() + padding * 2).coerceIn(1, constraints.maxWidth)
                val height = rect.height.roundToInt() + if (header) 24.dp.roundToPx() else padding * 2
                val highlight = measurables[index * 2].measure(Constraints.fixed(width, height.coerceAtLeast(1)))
                placements += Triple(highlight, x, y)
                val maxLabelWidth = if (header) (constraints.maxWidth - margin * 3) / 2 else constraints.maxWidth - margin * 2
                val label = measurables[index * 2 + 1].measure(Constraints(maxWidth = maxLabelWidth.coerceAtLeast(1)))
                val labelX = if (annotation.endAligned) (rect.right.roundToInt() + padding - label.width).coerceAtLeast(margin) else x.coerceAtLeast(margin)
                val labelY = (y + height + 12.dp.roundToPx()).coerceAtMost((maxLabelBottom - label.height).coerceAtLeast(0))
                placements += Triple(label, labelX.coerceAtMost((constraints.maxWidth - margin - label.width).coerceAtLeast(0)), labelY)
            }
            if (step == HomeTutorialStep.MISSION && HomeTutorialTarget.CARD in anchors) {
                val card = anchors.getValue(HomeTutorialTarget.CARD)
                val flip = anchors[HomeTutorialTarget.FLIP] ?: card
                val extraIndex = annotations.size * 2
                val hint = measurables[extraIndex].measure(Constraints(maxWidth = (constraints.maxWidth - margin * 2).coerceAtLeast(1)))
                val spotSize = 34.dp.roundToPx()
                val spotlight = measurables[extraIndex + 1].measure(Constraints.fixed(spotSize, spotSize))
                val lineHeight = 28.dp.roundToPx()
                val line = measurables[extraIndex + 2].measure(Constraints.fixed(1.dp.roundToPx(), lineHeight))
                val spotX = flip.center.x.roundToInt() - spotSize / 2
                val spotY = flip.center.y.roundToInt() - spotSize / 2
                placements += Triple(spotlight, spotX, spotY)
                placements += Triple(line, flip.center.x.roundToInt(), spotY - lineHeight)
                placements += Triple(hint, (card.right.roundToInt() - hint.width).coerceAtLeast(margin), (spotY - lineHeight - hint.height - padding).coerceAtLeast(margin))
            }
            layout(constraints.maxWidth, constraints.maxHeight) { placements.forEach { (child, x, y) -> child.place(x, y) } }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(KiriColor.Bg.Base).padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("home_tutorial_skip")) { Text("건너뛰기", color = KiriColor.Text.Secondary) }
            Text("${step.ordinal + 1} / 3", color = KiriColor.Text.Secondary, style = KiriDetailTypography.Small)
            Button(onClick = onNext, modifier = Modifier.testTag("home_tutorial_next")) { Text(if (step == HomeTutorialStep.RECOMMENDATIONS) "시작하기" else "다음") }
        }
    }
}
