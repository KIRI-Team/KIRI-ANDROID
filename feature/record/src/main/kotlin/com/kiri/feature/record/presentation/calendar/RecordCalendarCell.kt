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
import com.kiri.uikit.component.KiriAsyncImage
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
    // 오늘보다 이전 날짜
    past: Boolean = false,
) {
    // 사진(완료 후 리뷰·활동 기록)이 있는 날은 퍼즐 조각 모양으로 자른다 (Figma 10693:9281 "완료후 리뷰있는 약속")
    val shape = if (photo != null) ReviewPuzzleShape else RoundedCornerShape(12.dp)
    val schedule = scheduleCellLook(mark, today, past)
    // 사진 가운데가 밝으면 검정 숫자(추억기록날짜_black), 어두우면 흰 숫자(추억기록날짜_white). 불러오기 전에는 흰 숫자
    var darkNumber by remember(photo?.source) { mutableStateOf(false) }
    // 캘린더 요소 정리 (Figma 10693:9281). 오늘은 초록 배경과 초록 테두리, 예정된 약속은 Tertiary, 지난 약속은 Secondary
    val background =
        when {
            today -> KiriColor.Bg.GreenSoft
            mark == RecordCalendarMark.QUESTION ||
                mark == RecordCalendarMark.PHOTO_FIRST ||
                mark == RecordCalendarMark.PHOTO_SECOND -> KiriColor.Bg.Base
            schedule == ScheduleCellLook.UPCOMING -> KiriColor.Bg.Tertiary
            schedule == ScheduleCellLook.PAST || mark == RecordCalendarMark.BUCKET -> KiriColor.Bg.Secondary
            else -> KiriColor.Bg.Primary
        }
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(background)
            .then(
                // 초록 테두리는 오늘만 뜻한다. 고른 날짜는 따로 표시하지 않는다
                if (today) Modifier.border(1.dp, KiriColor.Primary.Default, shape) else Modifier
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
                            RecordCalendarMark.CANCELLED -> ", 취소·미확정 약속"
                            RecordCalendarMark.BUCKET -> ", 버킷 활동"
                            RecordCalendarMark.QUESTION -> ", 가족 질문"
                            RecordCalendarMark.PHOTO_FIRST,
                            RecordCalendarMark.PHOTO_SECOND -> ", 활동 기록"
                            null -> ""
                        }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) CalendarPhoto(photo, date) { darkNumber = it }
        else
            when (mark) {
                RecordCalendarMark.QUESTION ->
                    Image(
                        painterResource(R.drawable.record_question),
                        null,
                        Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                // 예정된 약속은 밝은 시계, 지난 약속은 어두운 시계, 오늘 약속은 초록 시계 (Figma 10693:9281).
                // 취소·미확정 약속은 열 수 없으므로 지난 약속처럼 어두운 시계로 보여준다
                RecordCalendarMark.PLANNED,
                RecordCalendarMark.CONFIRMED,
                RecordCalendarMark.CANCELLED ->
                    Image(
                        painterResource(
                            when (schedule) {
                                ScheduleCellLook.TODAY -> R.drawable.record_clock_today
                                ScheduleCellLook.UPCOMING -> R.drawable.record_clock_confirmed
                                else -> R.drawable.record_clock_planned
                            }
                        ),
                        null,
                        Modifier.size(33.6.dp).then(
                            if (mark == RecordCalendarMark.CANCELLED) Modifier.testTag("record_cancelled_$date") else Modifier
                        ),
                    )
                RecordCalendarMark.BUCKET ->
                    Image(painterResource(R.drawable.record_bucket), null, Modifier.size(33.dp))
                RecordCalendarMark.PHOTO_FIRST,
                RecordCalendarMark.PHOTO_SECOND -> Unit
                null -> Unit
            }
        if (
            photo != null ||
                mark == null ||
                mark == RecordCalendarMark.QUESTION ||
                mark == RecordCalendarMark.PHOTO_FIRST ||
                mark == RecordCalendarMark.PHOTO_SECOND
        ) {
            Text(
                date.dayOfMonth.toString(),
                Modifier.testTag("record_day_number_$date"),
                color =
                    when {
                        photo != null && darkNumber -> KiriColor.Text.Inverse
                        photo != null -> KiriColor.Text.Primary
                        mark == RecordCalendarMark.PHOTO_SECOND -> KiriColor.Text.Strong
                        else -> KiriColor.Text.Primary
                    },
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

// 날짜 칸을 채우는 사진. 서버·기기 사진은 불러온 뒤 가운데 밝기를 재서 숫자 색을 정하게 알려준다.
// 미리보기용 기본 사진은 밝기를 재지 않고 흰 숫자로 둔다
@Composable
private fun CalendarPhoto(photo: RecordPhoto, date: LocalDate, onDarkNumber: (Boolean) -> Unit) {
    val modifier = Modifier.fillMaxSize().testTag("record_day_photo_$date")
    if (photo.source.startsWith("demo:")) {
        RecordPhotoImage(photo, modifier)
        return
    }
    val report by rememberUpdatedState(onDarkNumber)
    KiriAsyncImage(
        photo.source,
        "활동 사진",
        modifier.background(KiriColor.Bg.Tertiary),
        onBitmap = { bitmap -> report(prefersDarkDayNumber(bitmap.centerLuminance())) },
    )
}
