package com.kiri.domain.home

// 로그인한 사용자의 가족 공간 이용 상태
enum class FamilyAccess {
    // 가족 공간을 만든 방장
    OWNER,
    // 초대로 가족 공간에 참여한 구성원
    MEMBER,
    // 가족 공간 없이 둘러보는 중. 가족 기능(기록 탭, 가족 설정 등)을 쓸 수 없다
    BROWSING,
}

// 가족 정보로 이용 상태를 정한다. 가족 이름이 없으면(소속된 가족 없음) 둘러보기다.
// 방장을 모르면 참여자로 본다 (방장 전용 기능만 서버가 다시 막는다)
val FamilyHome.access: FamilyAccess
    get() = when {
        browsing -> FamilyAccess.BROWSING
        ownerId != null && ownerId == currentUserId -> FamilyAccess.OWNER
        else -> FamilyAccess.MEMBER
    }

// 지금 둘러보기 중인지. 가족 정보를 불러오지 못하면 막지 않도록 false 로 본다
suspend fun HomeRepository.isBrowsing(): Boolean = try {
    family().access == FamilyAccess.BROWSING
} catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (_: Exception) {
    false
}

// 둘러보기 중에 가족 기능을 누르면 보여줄 안내
const val BROWSING_BLOCKED_MESSAGE = "가족에 참여하면 이용할 수 있어요."
