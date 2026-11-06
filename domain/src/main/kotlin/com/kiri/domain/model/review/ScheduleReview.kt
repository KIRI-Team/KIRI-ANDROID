package com.kiri.domain.model.review

// 일정을 수행한 가족 구성원이 남기는 리뷰. 사진은 미리 올려 받은 이미지 키로 보낸다 (최소 1장)
data class ScheduleReviewForm(
    // 별점 1 ~ 5
    val starRating: Int,
    val content: String,
    // 이 일정에 다시 참여할 의사
    val rejoinIntended: Boolean,
    val imageKeys: List<String>,
) {
    // 서버로 보내기 전 앱에서 먼저 거르는 조건. 내용 길이 제한은 명세에 없어 서버 검증에 맡긴다
    val canSubmit: Boolean
        get() = starRating in 1..5 && content.isNotBlank() && imageKeys.isNotEmpty() && imageKeys.none { it.isBlank() }
}

data class ScheduleReviewResult(val reviewId: Long, val scheduleId: Long)

// 해당 일정에 참여한 가족 구성원이 아니다
class NotScheduleParticipantException : Exception("해당 일정에 참여한 가족 구성원이 아닙니다.")

// 이 일정에 이미 리뷰를 작성했다
class AlreadyReviewedException : Exception("이미 리뷰를 작성했습니다.")

// 필수값 누락, 별점 범위, 내용 길이, 이미지 미등록 등 입력 오류. 서버가 준 안내 문구를 담는다
class InvalidReviewException(message: String) : Exception(message)
