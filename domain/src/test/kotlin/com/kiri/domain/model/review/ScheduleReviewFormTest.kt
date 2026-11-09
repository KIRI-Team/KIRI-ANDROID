package com.kiri.domain.model.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 리뷰는 별점(1~5), 내용, 사진 1장 이상이 있어야 보낼 수 있다
class ScheduleReviewFormTest {
    private val form = ScheduleReviewForm(4, "즐거웠어요", rejoinIntended = false, imageKeys = listOf("reviews/1.jpg"))

    @Test
    fun 별점_내용_사진이_모두_있어야_보낼_수_있다() {
        assertTrue(form.canSubmit)
        listOf(
            form.copy(starRating = 0),
            form.copy(starRating = 6),
            form.copy(content = "  "),
            form.copy(imageKeys = emptyList()),
            form.copy(imageKeys = listOf(" ")),
        ).forEach { assertFalse(it.canSubmit) }
    }
}
