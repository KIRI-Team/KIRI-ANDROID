package com.kiri.domain.schedule.model

import com.kiri.domain.member.model.FamilyRole
import java.time.LocalDate
import java.time.LocalTime

// 일정 상세. 같은 가족의 일정이면 초대받지 않았어도 볼 수 있다.
// 초대받은 화면(요청알림), 일정 변경알림, 약속 확정·당일 화면, 일정 취소 확인 화면을 이 값 하나로 그린다
data class ScheduleDetail(
    val date: LocalDate,
    // 읽지 못하면 null
    val time: LocalTime?,
    // 앱이 모르는 값이면 null
    val status: ScheduleDisplayStatus?,
    // 제안자가 맨 앞이고 이후 초대 순서다. 탈퇴한 회원의 닉네임은 "탈퇴한 사용자"로 온다
    val participants: List<ScheduleParticipant>,
    // 이 일정의 미션. 미션 상세(장소·활동·준비물)를 불러올 때 쓴다. 오지 않으면 null
    val missionId: Long? = null,
    // 미션명과 행사 설명. 행사가 취소·숨김 처리돼 미션 상세가 404 여도 온다. 없으면 null
    val missionTitle: String? = null,
    val missionDescription: String? = null,
    // 일정 제안자. 오지 않으면 null
    val proposer: ScheduleProposer? = null,
    // 가족에게 한 줄. 없으면 null
    val requestMessage: String? = null,
    // 내 응답 상태 (캘린더·다가오는 일정의 myStatus 와 같은 값). 앱이 모르는 값이면 null
    val myStatus: MyScheduleStatus? = null,
    // 재확인이 필요할 때(RECONFIRM_REQUIRED) 바뀐 항목. 그 외에는 비어 있다
    val changedFields: Set<ScheduleChangedField> = emptySet(),
    // 수락·거절 / 일정 변경 / 일정 취소 버튼을 보여줄지.
    // 서버가 제안자 여부, 일정 상태, 응답 마감, 시작 여부까지 판단한다
    val canRespond: Boolean = false,
    val canEdit: Boolean = false,
    val canCancel: Boolean = false,
) {
    // 처음 받은 초대(요청알림)
    val isFirstInvitation: Boolean
        get() = myStatus == MyScheduleStatus.PENDING && canRespond

    // 일정이 바뀌어 다시 응답해야 한다(일정 변경알림). 화면을 열기만 해서는 풀리지 않고, 다시 응답해야 풀린다
    val needsReconfirm: Boolean
        get() = myStatus == MyScheduleStatus.RECONFIRM_REQUIRED
}

data class ScheduleProposer(
    val memberId: Long,
    val nickname: String,
    // 역할이 없거나 앱이 모르는 값이면 null
    val familyRole: FamilyRole?,
)

data class ScheduleParticipant(
    val nickname: String,
    // 초대 응답 상태. 앱이 모르는 값이면 null
    val status: MyScheduleStatus?,
    // 초대 수락 현황에 쓰는 역할별 캐릭터 이미지. 역할이 없거나 탈퇴했으면 null
    val characterImageUrl: String? = null,
) {
    val accepted: Boolean
        get() = status == MyScheduleStatus.ACCEPTED
}

// 일정 변경으로 바뀐 항목. "변경되었어요!" 배지를 이 항목 옆에 단다
enum class ScheduleChangedField { DATE, TIME, PARTICIPANTS }

// 화면 표시용 일정 상태
enum class ScheduleDisplayStatus(val label: String) {
    SCHEDULED("약속 예정"),
    CONFIRMED("약속 확정"),
    TODAY("약속 당일이에요!"),
    COMPLETED("약속 완료"),
}

// 없거나 다른 가족의 일정이다
class ScheduleNotFoundException : Exception("일정을 찾을 수 없습니다.")

// 취소된 일정은 상세를 볼 수 없다
class CancelledScheduleException : Exception("취소된 일정입니다.")
