package com.kiri.feature.record.presentation.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.kiri.domain.record.RecordPhoto
import com.kiri.feature.record.R
import com.kiri.feature.record.model.*
import com.kiri.feature.record.presentation.RecordPhotoImage
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriDetailTypography
import java.time.LocalDate

@Composable
internal fun CalendarCell(
    date: LocalDate,
    mark: RecordCalendarMark?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    photo: RecordPhoto? = null,
    count: Int = 0,
    today: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    // 초록 배경은 오늘 날짜만 뜻한다. 다른 날은 약속 상태와 관계없이 초록 배경을 쓰지 않는다
    val background =
        when {
            today -> KiriColor.Bg.GreenSoft
            mark == RecordCalendarMark.QUESTION ||
                mark == RecordCalendarMark.PHOTO_FIRST ||
                mark == RecordCalendarMark.PHOTO_SECOND -> KiriColor.Bg.Base
            mark == RecordCalendarMark.CANCELLED -> KiriColor.Bg.Secondary.copy(alpha = .4f)
            mark == RecordCalendarMark.PLANNED ||
                mark == RecordCalendarMark.CONFIRMED ||
                mark == RecordCalendarMark.BUCKET -> KiriColor.Bg.Secondary
            else -> KiriColor.Bg.Primary
        }
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(background)
            .then(
                if (selected) Modifier.border(1.dp, KiriColor.Primary.Default, shape) else Modifier
            )
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("record_date_$date")
            .semantics {
                this.selected = selected
                contentDescription =
                    "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일" +
                        (if (today) ", 오늘" else "") +
                        when (mark) {
                            RecordCalendarMark.PLANNED -> ", 약속 예정"
                            RecordCalendarMark.CONFIRMED -> ", 약속 확정"
                            RecordCalendarMark.CANCELLED -> ", 취소된 약속"
                            RecordCalendarMark.BUCKET -> ", 버킷 활동"
                            RecordCalendarMark.QUESTION -> ", 가족 질문"
                            RecordCalendarMark.PHOTO_FIRST,
                            RecordCalendarMark.PHOTO_SECOND -> ", 활동 기록"
                            null -> ""
                        }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) RecordPhotoImage(photo, Modifier.fillMaxSize())
        else
            when (mark) {
                RecordCalendarMark.QUESTION ->
                    Image(
                        painterResource(R.drawable.record_question),
                        null,
                        Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                RecordCalendarMark.PLANNED ->
                    Image(
                        painterResource(R.drawable.record_clock_planned),
                        null,
                        Modifier.size(33.6.dp),
                    )
                RecordCalendarMark.CONFIRMED ->
                    Image(
                        painterResource(R.drawable.record_clock_confirmed),
                        null,
                        Modifier.size(33.6.dp),
                    )
                // 취소된 약속은 흐리게 보여준다
                RecordCalendarMark.CANCELLED ->
                    Image(
                        painterResource(R.drawable.record_clock_planned),
                        null,
                        Modifier.size(33.6.dp).alpha(.4f).testTag("record_cancelled_$date"),
                    )
                RecordCalendarMark.BUCKET ->
                    Image(painterResource(R.drawable.record_bucket), null, Modifier.size(33.dp))
                RecordCalendarMark.PHOTO_FIRST,
                RecordCalendarMark.PHOTO_SECOND -> Unit
                null -> Unit
            }
        if (
            mark == null ||
                mark == RecordCalendarMark.QUESTION ||
                mark == RecordCalendarMark.PHOTO_FIRST ||
                mark == RecordCalendarMark.PHOTO_SECOND
        ) {
            Text(
                date.dayOfMonth.toString(),
                color =
                    if (mark == RecordCalendarMark.PHOTO_SECOND) KiriColor.Text.Strong
                    else KiriColor.Text.Primary,
                style = KiriDetailTypography.Body,
            )
        }
        if (count > 1)
            Text(
                "+${count - 1}",
                Modifier.align(Alignment.TopEnd)
                    .background(KiriColor.Bg.Secondary, RoundedCornerShape(4.dp))
                    .padding(2.dp)
                    .testTag("record_day_count_$date"),
                color = KiriColor.Text.Strong,
                style = KiriDetailTypography.Small,
            )
    }
}
