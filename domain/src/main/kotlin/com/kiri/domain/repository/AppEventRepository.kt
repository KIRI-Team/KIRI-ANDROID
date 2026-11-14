package com.kiri.domain.repository

interface AppEventRepository {
    suspend fun recordMissionDetailView(missionId: Long): Result<Unit>
}
