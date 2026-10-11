package com.kiri.domain.detail

import org.junit.Assert.*
import org.junit.Test

class NearbyRankingTest {
    private val places = listOf(
        NearbyPlace("far-free", "A", "", "", 900, false, 0),
        NearbyPlace("near-rest", "B", "", "", 100, true, 5000),
        NearbyPlace("unknown", "C", "", ""),
    )
    @Test fun noSelectionPreservesSourceOrder() { assertEquals(places, rankNearby(places, emptySet())) }
    @Test fun walkingAndRestPromoteSuitableKnownCandidates() {
        assertEquals("near-rest", rankNearby(places, setOf(Condition.LESS_WALKING, Condition.MORE_REST)).first().id)
        assertEquals("unknown", rankNearby(places, setOf(Condition.LESS_WALKING)).last().id)
    }
    @Test fun budgetPromotesFreePlaces() { assertEquals("far-free", rankNearby(places, setOf(Condition.LOWER_BUDGET)).first().id) }
    @Test fun crowdConditionDoesNotInventMissingData() { assertEquals(places, rankNearby(places, setOf(Condition.AVOID_CROWDS))) }
    @Test fun reliableCrowdDataIsUsedAndTiesAreStable() {
        val candidates = places.mapIndexed { index, p -> p.copy(crowdLevel = if (index == 0) 3 else 1) }
        assertEquals(listOf("near-rest", "unknown", "far-free"), rankNearby(candidates, setOf(Condition.AVOID_CROWDS)).map { it.id })
    }
    @Test fun emptyCandidatesAreSafe() { assertTrue(rankNearby(emptyList(), Condition.entries.toSet()).isEmpty()) }
}
