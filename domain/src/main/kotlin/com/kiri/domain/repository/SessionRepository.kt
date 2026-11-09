package com.kiri.domain.repository

enum class SessionDestination { LOGIN, TERMS, ONBOARDING, MAIN }
data class ProfileProgress(val nickname: String? = null, val role: String? = null)
interface SessionRepository {
    suspend fun restore(): Result<SessionDestination>
    suspend fun profile(): ProfileProgress
    suspend fun saveNickname(value: String)
    suspend fun saveRole(value: String)
    suspend fun completeOnboarding()
}
