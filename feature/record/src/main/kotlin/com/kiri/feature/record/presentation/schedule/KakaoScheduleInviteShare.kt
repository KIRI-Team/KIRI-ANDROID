package com.kiri.feature.record.presentation.schedule

import android.content.Context
import android.util.Log
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate
import com.kiri.domain.schedule.model.ScheduleInviteLink

private const val TAG = "ScheduleInviteShare"

internal const val KAKAO_INVITE_UNAVAILABLE_MESSAGE = "카카오톡으로 초대장을 보내지 못했어요. 가족에게는 앱 알림으로 전달돼요."

// 일정 초대장을 카카오톡으로 보낸다. 문구는 일정 제목 없이 공통 문구만 쓰고,
// 버튼은 실행 파라미터 scheduleId 로 앱을 연다 (앱이 없으면 카카오톡이 스토어로 보낸다)
// 카카오톡이 없거나 SDK 를 쓸 수 없으면 onFailure 로 알린다. 일정은 이미 만들어졌고 초대 푸시도 간다
internal fun shareScheduleInviteByKakao(context: Context, scheduleId: Long, onFailure: (String) -> Unit) {
    val share = ShareClient.instance
    // 키가 없어 카카오 SDK 를 초기화하지 않은 환경에서는 예외가 난다
    val available = runCatching { share.isKakaoTalkSharingAvailable(context) }.getOrDefault(false)
    if (!available) return onFailure(KAKAO_INVITE_UNAVAILABLE_MESSAGE)
    val params = ScheduleInviteLink.executionParams(scheduleId)
    val template = TextTemplate(
        text = ScheduleInviteLink.MESSAGE,
        link = Link(androidExecutionParams = params, iosExecutionParams = params),
        buttonTitle = ScheduleInviteLink.BUTTON_TITLE,
    )
    share.shareDefault(context, template) { result, error ->
        if (result != null) {
            context.startActivity(result.intent)
        } else {
            Log.w(TAG, "카카오톡 일정 초대장 공유 실패", error)
            onFailure(KAKAO_INVITE_UNAVAILABLE_MESSAGE)
        }
    }
}
