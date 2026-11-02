package com.kiri.feature.home.model

sealed interface HomeUiEvent {
    data object TutorialNext : HomeUiEvent
    data object TutorialDismissed : HomeUiEvent
    data object RetryClicked : HomeUiEvent
    data object DateRefreshRequested : HomeUiEvent
    data class MemberSelected(val id: String) : HomeUiEvent
    data object AnswerEditClicked : HomeUiEvent
    data class AnswerChanged(val text: String) : HomeUiEvent
    data object AnswerSaveClicked : HomeUiEvent
    data object PokeClicked : HomeUiEvent
    data object CardFlipped : HomeUiEvent
    data object BackClicked : HomeUiEvent
    data object NoticeDismissed : HomeUiEvent
    data object MemberDismissed : HomeUiEvent
    data class DestinationClicked(val destination: HomeUiDestination) : HomeUiEvent
    data class MissionClicked(val id: String) : HomeUiEvent
    data object WeeklyScheduleClicked : HomeUiEvent
    data class ScheduleClicked(val id: String) : HomeUiEvent
    data object InviteFamilyClicked : HomeUiEvent
}
