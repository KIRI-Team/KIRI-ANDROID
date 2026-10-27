package com.kiri.data.detail

import com.kiri.data.mission.FakeMissionRepository
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.schedule.ScheduleRepository
import java.time.Clock

/** Explicit fixtures for tests/previews, separated from the application binding. */
class FakeDetailRepository(
    homeRepository: HomeRepository,
    missionRepository: MissionRepository = FakeMissionRepository(),
    schedules: ScheduleRepository? = null,
    clock: Clock = Clock.systemUTC(),
) : LocalDetailRepository(homeRepository, missionRepository, schedules, clock)
