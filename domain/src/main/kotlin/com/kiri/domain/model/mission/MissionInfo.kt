package com.kiri.domain.model.mission

import java.time.LocalDate
import java.time.LocalTime

// 서버 미션 상세. 미션과 장소, 필수 활동(준비물·단계), 공식 출처를 담는다.
// 공개 중이거나 기간이 끝난 미션만 받을 수 있다
data class MissionInfo(
    val id: Long,
    val title: String,
    val description: String?,
    // EXHIBITION 처럼 서버 값 그대로 둔다. 앱이 모르는 값이 와도 상세는 보여줄 수 있다
    val category: String?,
    val missionType: String?,
    val status: MissionStatus?,
    val indoorOutdoor: String?,
    // null 이면 기본 이미지를 쓴다. 출처는 이미지가 있을 때만 온다
    val imageUrl: String?,
    val imageSource: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    // 미션 값이 없으면 장소 운영시간이 오고, 둘 다 없으면 null
    val openingTime: LocalTime?,
    val closingTime: LocalTime?,
    // 0 이면 무료, null 이면 정보 없음
    val price: Int?,
    // 예상 활동시간 범위(분)
    val durationMinMinutes: Int?,
    val durationMaxMinutes: Int?,
    // 판단할 정보가 없거나 앱이 모르는 값이면 null
    val walkingLevel: WalkingLevel?,
    val sourceUrl: String?,
    val place: MissionPlace?,
    val prepareItems: List<MissionGuide>,
    val steps: List<MissionGuide>,
    // 로그인한 본인의 버킷 저장 상태. 가족이 없으면 NONE
    val bucketState: MissionBucketState,
    // 리뷰 기능이 연결되기 전까지 null·0
    val averageRating: Double?,
    val reviewCount: Int,
) {
    val isFree get() = price == 0
}

enum class MissionStatus { ACTIVE, ENDED }

enum class WalkingLevel(val label: String) { LOW("적음"), MEDIUM("보통"), HIGH("많음") }

// 버킷 저장 버튼 문구: NONE "버킷에 저장", SAVED "저장됨"(SAVED·SCHEDULED 버킷이 있음), COMPLETED_ONLY "다시 버킷에 저장"
enum class MissionBucketState(val label: String) {
    NONE("버킷에 저장"),
    SAVED("저장됨"),
    COMPLETED_ONLY("다시 버킷에 저장"),
}

data class MissionPlace(
    val id: Long,
    val name: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
)

// 준비물이나 활동 단계 하나
data class MissionGuide(val title: String, val description: String?)

// 없는 미션이거나 공개 중·기간 종료가 아닌 미션이다
class MissionNotFoundException : Exception("존재하지 않는 미션입니다.")
