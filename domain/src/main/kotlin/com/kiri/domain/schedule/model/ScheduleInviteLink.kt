package com.kiri.domain.schedule.model

// 일정 초대는 카카오톡 공유(SDK)로만 보낸다. 받는 사람은 이미 같은 가족이라 앱이 설치돼 있고 푸시도 함께 받는다.
// 카톡 메시지의 버튼은 앱을 kakao{네이티브 앱 키}://kakaolink?scheduleId={id} 로 연다. 앱이 없으면 카카오톡이 스토어로 보낸다.
// 다른 가족이 링크를 받아도 일정 상세를 조회할 수 없고, 수락·거절은 초대받은 사람만 할 수 있다 (서버에서 막는다)
object ScheduleInviteLink {
    // 카톡 버튼의 실행 파라미터 이름
    const val SCHEDULE_ID_PARAM = "scheduleId"

    // 카카오톡이 실행 파라미터로 앱을 열 때 쓰는 host. AndroidManifest의 intent-filter와 같은 값이어야 한다
    const val KAKAO_LINK_HOST = "kakaolink"

    // 일정 제목 없이 보내는 공통 문구
    const val MESSAGE = "초대장이 도착했어요"
    const val BUTTON_TITLE = "초대장 보기"

    private const val KAKAO_SCHEME_PREFIX = "kakao"

    fun executionParams(scheduleId: Long): Map<String, String> = mapOf(SCHEDULE_ID_PARAM to scheduleId.toString())

    // 일정 초대 링크가 아니거나 일정 id 가 올바르지 않으면 null
    fun parseScheduleId(scheme: String?, host: String?, scheduleId: String?): Long? {
        if (scheme == null || !scheme.startsWith(KAKAO_SCHEME_PREFIX) || host != KAKAO_LINK_HOST) return null
        return scheduleId?.toLongOrNull()?.takeIf { it > 0 }
    }
}
