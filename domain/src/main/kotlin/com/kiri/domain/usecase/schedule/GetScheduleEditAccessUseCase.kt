package com.kiri.domain.usecase.schedule

import com.kiri.domain.home.FamilyHome
import com.kiri.domain.home.FamilyMember
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.home.invitableMembers
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.canBeEditedBeforeTheDay
import com.kiri.domain.model.schedule.isProposedBy
import com.kiri.domain.repository.CalendarScheduleRepository
import kotlinx.coroutines.CancellationException

// 일정 상세와, 내가 그 일정을 바꿀 수 있는지를 함께 알려준다.
// 일정 상세 화면(제안자면 변경 화면으로 보낸다)과 일정 변경 화면(기존 값을 채운다)이 같은 판단을 쓴다
class GetScheduleEditAccessUseCase(
    private val schedules: CalendarScheduleRepository,
    private val home: HomeRepository,
) {
    // 상세를 불러오지 못하면 그 오류로 실패한다.
    // 가족 정보는 바꿀 수 있는 상태(예정·확정)일 때만 불러오고, 불러오지 못하면 family 를 null 로 둔다
    suspend operator fun invoke(scheduleId: Long): Result<ScheduleEditAccess> =
        schedules.getScheduleDetail(scheduleId).map { detail ->
            ScheduleEditAccess(detail, if (detail.canBeEditedBeforeTheDay) loadFamily() else null)
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
    // 불러오지 못했거나 바꿀 수 없는 상태라 불러오지 않았으면 null
    val family: FamilyHome?,
) {
    val me: FamilyMember?
        get() = family?.let { f -> f.members.find { it.id == f.currentUserId } }

    // 참여자 맨 앞이 제안자다. 닉네임은 회원 사이에 겹치지 않으므로 내 닉네임과 비교한다
    val isMine: Boolean
        get() = me?.let { detail.isProposedBy(it.nickname) } == true

    // 당일이 아니고(예정·확정) 내가 제안한 일정이면 바꿀 수 있다
    val canEdit: Boolean
        get() = detail.canBeEditedBeforeTheDay && isMine

    // 초대할 수 있는 가족. 나와 탈퇴한 구성원은 뺀다
    val invitableMembers: List<FamilyMember>
        get() = family?.invitableMembers().orEmpty()

    // 지금 초대된 가족의 회원 id. 상세의 참여자는 닉네임뿐이라 닉네임이 같은 가족을 찾는다 (맨 앞의 제안자는 뺀다)
    val invitedMemberIds: Set<String>
        get() = detail.participants.drop(1)
            .mapNotNull { p -> invitableMembers.find { it.nickname == p.nickname }?.id }
            .toSet()
}
