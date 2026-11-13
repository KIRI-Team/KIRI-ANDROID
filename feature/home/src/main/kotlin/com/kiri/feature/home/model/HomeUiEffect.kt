package com.kiri.feature.home.model

sealed interface HomeUiEffect {
    data class Navigate(val destination: HomeUiDestination) : HomeUiEffect
    data class OpenMission(val id: String) : HomeUiEffect
    data class OpenSchedule(val id: String, val respond: Boolean = false) : HomeUiEffect
    data class CreateWeeklySchedule(val weeklyMissionId: String) : HomeUiEffect
    data class InviteFamily(val code: String) : HomeUiEffect
}
