package com.kiri.domain.schedule.usecase

import com.kiri.domain.home.repository.FamilyHome
import com.kiri.domain.home.repository.FamilyMember
import com.kiri.domain.home.repository.HomeRepository
import com.kiri.domain.home.repository.invitableMembers
import com.kiri.domain.schedule.model.ScheduleDetail
import com.kiri.domain.schedule.repository.CalendarScheduleRepository
import kotlinx.coroutines.CancellationException

// 일정 상세와, 일정 변경 화면에 채울 가족 정보를 함께 알려준다.
// 일정 상세 화면(일정 변경하기 버튼)과 일정 변경 화면(기존 값을 채운다)이 같은 판단을 쓴다.
// 바꿀 수 있는지는 서버가 상세의 canEdit 로 판단해 준다
class GetScheduleEditAccessUseCase(
    private val schedules: CalendarScheduleRepository,
    private val home: HomeRepository,
) {
    // 상세를 불러오지 못하면 그 오류로 실패한다.
    // 가족 정보는 바꿀 수 있는 일정(canEdit)일 때만 불러오고, 불러오지 못하면 family 를 null 로 둔다
    suspend operator fun invoke(scheduleId: Long): Result<ScheduleEditAccess> =
        schedules.getScheduleDetail(scheduleId).map { detail ->
            ScheduleEditAccess(detail, if (detail.canEdit) loadFamily() else null)
        }

    private suspend fun loadFamily(): FamilyHome? = try {
        home.family()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}

data class ScheduleEditAccess(
    val detail: ScheduleDetail,
    // 불러오지 못했거나 바꿀 수 없는 일정이라 불러오지 않았으면 null
    val family: FamilyHome?,
) {
    val me: FamilyMember?
        get() = family?.let { f -> f.members.find { it.id == f.currentUserId } }

    // 서버가 제안자 여부·일정 상태·시작 여부까지 판단한 값
    val canEdit: Boolean
        get() = detail.canEdit

    // 초대할 수 있는 가족. 나와 탈퇴한 구성원은 뺀다
    val invitableMembers: List<FamilyMember>
        get() = family?.invitableMembers().orEmpty()

    // 지금 초대된 가족의 회원 id. 상세의 참여자는 닉네임뿐이라 닉네임이 같은 가족을 찾는다 (제안자는 뺀다)
    val invitedMemberIds: Set<String>
        get() {
            val proposer = detail.proposer?.nickname ?: detail.participants.firstOrNull()?.nickname
            return detail.participants
                .filter { it.nickname != proposer }
                .mapNotNull { p -> invitableMembers.find { it.nickname == p.nickname }?.id }
                .toSet()
        }
}
