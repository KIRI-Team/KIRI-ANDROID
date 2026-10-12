package com.kiri.domain.home

/** Atomically consumes the first successful family-home visit for a user on this installation. */
interface HomeTutorialRepository {
    suspend fun claimFirstVisit(userId: String): Boolean
}

/** Explicit opt-out for previews and tests unrelated to onboarding. Production uses local storage. */
object NoHomeTutorial : HomeTutorialRepository {
    override suspend fun claimFirstVisit(userId: String) = false
}
