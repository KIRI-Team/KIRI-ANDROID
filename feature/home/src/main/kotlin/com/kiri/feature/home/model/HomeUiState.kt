package com.kiri.feature.home.model

import com.kiri.domain.home.*

data class HomeUiState(
    val loading: Boolean = true,
    val home: FamilyHome? = null,
    // 홈을 불러오지 못한 이유 (불러왔거나 불러오는 중이면 null)
    val loadFailure: HomeLoadFailure? = null,
    val selectedMemberId: String? = null,
    val editing: Boolean = false,
    val draft: String = "",
    val draftQuestionId: String? = null,
    val showingMission: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val answerSaved: Boolean = false,
    val tutorialStep: HomeTutorialStep? = null,
) {
    val loadError: Boolean get() = loadFailure != null

    val canSave: Boolean get() = editing && draft.isNotBlank() && draft.length <= 500 && !busy && home?.question != null
}

// 홈 오류 화면에 보여줄 안내. 연결 문제·서버 오류·로그인 만료를 구분한다
enum class HomeLoadFailure(val message: String) {
    NETWORK("인터넷 연결을 확인하고 다시 시도해주세요."),
    SERVER("서버에 잠시 문제가 있어요. 잠시 후 다시 시도해주세요."),
    SESSION("로그인 정보를 확인하지 못했어요. 다시 시도해주세요."),
    UNKNOWN("홈을 불러오지 못했어요. 다시 시도해주세요."),
    ;

    companion object {
        fun from(error: Throwable): HomeLoadFailure = when ((error as? HomeException)?.reason) {
            HomeFailure.NETWORK -> NETWORK
            HomeFailure.SERVER -> SERVER
            HomeFailure.SESSION_EXPIRED -> SESSION
            else -> if (error is java.io.IOException) NETWORK else UNKNOWN
        }
    }
}
