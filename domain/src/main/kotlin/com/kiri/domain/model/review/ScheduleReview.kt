package com.kiri.domain.model.review

import com.kiri.domain.model.member.FamilyRole
import java.time.LocalDateTime

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

// 일정 참여자가 작성한 리뷰 한 건. 기록 완료 화면의 '가족 추억' 탭에서 보여준다
data class ScheduleReview(
    val id: Long,
    val writer: ReviewWriter,
    val content: String,
    // 1 ~ 5
    val starRating: Int,
    val rejoinIntended: Boolean,
    // 작성자가 올린 사진 주소. 올린 순서대로다
    val imageUrls: List<String>,
    // 읽지 못하면 null
    val createdAt: LocalDateTime?,
    // 내가 쓴 리뷰면 수정하기를 보여준다
    val isMine: Boolean,
    // 사진 주소 → 저장된 사진 Key(user/original/...). 수정할 때 유지할 기존 사진은 이 Key 로 보낸다
    val photoKeys: Map<String, String> = emptyMap(),
)

data class ReviewWriter(
    val memberId: Long,
    val nickname: String,
    // 앱이 모르는 값이거나 정하지 않았으면 null
    val familyRole: FamilyRole?,
)

// 해당 일정에 참여한 가족 구성원이 아니다
class NotScheduleParticipantException : Exception("해당 일정에 참여한 가족 구성원이 아닙니다.")

// 이 일정에 이미 리뷰를 작성했다
class AlreadyReviewedException : Exception("이미 리뷰를 작성했습니다.")

// 내 가족의 일정이 아니라서(가족 공간 없음 포함) 리뷰를 볼 수 없다
class ReviewAccessDeniedException : Exception("해당 일정의 리뷰를 조회할 권한이 없습니다.")

// 수정하려는 리뷰가 없거나 그 일정의 리뷰가 아니다 (REVIEW404_1)
class ReviewNotFoundException : Exception("존재하지 않는 리뷰입니다.")

// 필수값 누락, 별점 범위, 내용 길이, 이미지 미등록 등 입력 오류. 서버가 준 안내 문구를 담는다
class InvalidReviewException(message: String) : Exception(message)
