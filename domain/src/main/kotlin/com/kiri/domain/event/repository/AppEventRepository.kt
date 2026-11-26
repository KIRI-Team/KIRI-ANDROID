package com.kiri.domain.event.repository

interface AppEventRepository {
    suspend fun recordMissionDetailView(missionId: Long): Result<Unit>
}
