package com.kiri.data.home

import com.kiri.domain.home.*
import java.time.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FakeHomeRepositoryTest {
    private val clock = MutableClock(Instant.parse("2026-09-30T14:59:59Z"))
    private fun repo(scenario: HomeScenario = HomeScenario.DEFAULT) = FakeHomeRepository(clock, scenario, 0)

    @Test fun blankAnswersAreRejectedWithoutChangingData() = runBlocking {
        val repo = repo()
        val before = repo.load()
        for (text in listOf("", " ", "\n\t", "　")) {
            assertFailure(HomeFailure.EMPTY_ANSWER) { repo.saveAnswer(before.question!!, text) }
        }
        assertEquals(before, repo.load())
    }

    @Test fun saveAndEditOnlyUpdateCurrentUserAndDoNotTruncate() = runBlocking {
        val repo = repo()
        val before = repo.load()
        repo.saveAnswer(before.question!!, "첫 답변")
        val text = "긴 답변 ".repeat(1000)
        val after = repo.saveAnswer(before.question!!, text)
        assertEquals(text, after.answers[before.currentUserId])
        assertEquals(before.answers, after.answers - before.currentUserId)
        assertEquals(before.mission, after.mission)
    }

    @Test fun saveFailurePreservesSnapshotAndAllowsRetry() = runBlocking {
        val repo = repo(HomeScenario.SAVE_FAILURE)
        val before = repo.load()
        assertFailure(HomeFailure.NETWORK) { repo.saveAnswer(before.question!!, "입력 유지") }
        assertEquals(before, repo.load())
        assertEquals("입력 유지", repo.saveAnswer(before.question!!, "입력 유지").answers["me"])
    }

    @Test fun pokeIsOncePerTargetAndDoesNotChangeAnswerOrMission() = runBlocking {
        val repo = repo()
        val before = repo.load()
        val after = repo.poke(before.question!!, "father")
        assertEquals(before.answers, after.answers)
        assertEquals(before.mission, after.mission)
        assertFailure(HomeFailure.ALREADY_POKED) { repo.poke(before.question!!, "father") }
        assertFailure(HomeFailure.ALREADY_ANSWERED) { repo.poke(before.question!!, "mother") }
        assertFailure(HomeFailure.UNAVAILABLE) { repo.poke(before.question!!, "me") }
        assertFailure(HomeFailure.UNAVAILABLE) { repo.poke(before.question!!, "unknown") }
    }

    @Test fun concurrentPokesHaveExactlyOneSuccess() = runBlocking {
        val repo = repo()
        val question = repo.load().question!!
        val results = List(8) { async { runCatching { repo.poke(question, "father") } } }.awaitAll()
        assertEquals(1, results.count { it.isSuccess })
    }

    @Test fun pokeFailureDoesNotConsumeAllowance() = runBlocking {
        val repo = repo(HomeScenario.POKE_FAILURE)
        val before = repo.load()
        assertFailure(HomeFailure.NETWORK) { repo.poke(before.question!!, "father") }
        assertEquals(before, repo.load())
        assertTrue(repo.poke(before.question!!, "father").pokedMemberIds.contains("father"))
    }

    @Test fun kstMidnightExpiresOldAssignmentAndResetsPokeAllowance() = runBlocking {
        val repo = repo()
        val before = repo.load()
        repo.poke(before.question!!, "father")
        clock.instant = Instant.parse("2026-09-30T15:00:00Z")
        assertFailure(HomeFailure.EXPIRED) { repo.saveAnswer(before.question!!, "어제 질문") }
        assertFailure(HomeFailure.EXPIRED) { repo.poke(before.question!!, "father") }
        val after = repo.load()
        assertEquals(LocalDate.of(2026, 10, 1), after.date)
        assertFalse(after.answers.containsKey("me"))
        assertTrue(after.canPoke("father"))
    }

    @Test fun browsingAndUnassignedDoNotExposeFamilyAnswers() = runBlocking {
        for (scenario in listOf(HomeScenario.BROWSING, HomeScenario.UNASSIGNED)) {
            val repo = repo(scenario)
            val home = repo.load()
            assertNull(home.question)
            assertTrue(home.answers.isEmpty())
            assertFalse(home.canRead("mother"))
            assertFalse(home.canPoke("father"))
            assertFailure(HomeFailure.UNAVAILABLE) { repo.saveAnswer(DailyQuestion("fake", home.date, "질문"), "답변") }
            if (scenario == HomeScenario.BROWSING) {
                assertTrue(home.members.isEmpty())
                assertNull(home.mission)
            }
        }
    }

    @Test fun departedMemberCannotBeReadOrPoked() = runBlocking {
        val repo = repo(HomeScenario.MANY_MEMBERS)
        val home = repo.load()
        assertFalse(home.canRead("left"))
        assertFailure(HomeFailure.UNAVAILABLE) { repo.poke(home.question!!, "left") }
    }

    private suspend fun assertFailure(reason: HomeFailure, block: suspend () -> Unit) {
        try { block(); fail("Expected $reason") } catch (error: HomeException) { assertEquals(reason, error.reason) }
    }

    private class MutableClock(var instant: Instant) : Clock() {
        override fun instant() = instant
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = object : Clock() {
            override fun instant() = this@MutableClock.instant
            override fun getZone() = zone
            override fun withZone(zone: ZoneId) = this@MutableClock.withZone(zone)
        }
    }
}
