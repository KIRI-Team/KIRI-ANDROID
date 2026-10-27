package com.kiri.data.record

import com.kiri.domain.detail.DetailRepository
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.record.BucketRepository
import com.kiri.domain.schedule.ScheduleRepository
import java.time.Clock

/** Explicit demo records for tests/previews. Real server-backed families never receive fixtures. */
class FakeRecordRepository(
    home: HomeRepository,
    missions: MissionRepository,
    scheduledDetails: DetailRepository = com.kiri.data.detail.FakeDetailRepository(home, missions),
    familySchedules: ScheduleRepository? = null,
    familyBuckets: BucketRepository? = null,
    clock: Clock = Clock.systemUTC(),
) :
    LocalRecordRepository(
        home,
        missions,
        scheduledDetails,
        familySchedules,
        familyBuckets,
        clock,
        seedDemo = true,
    )
