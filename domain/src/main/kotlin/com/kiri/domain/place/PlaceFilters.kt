package com.kiri.domain.place

/** Optional constraints. Empty regions and null fields mean unrestricted. */
data class PlaceFilters(
    val regions: Set<String> = emptySet(),
    val distanceKm: Int? = null,
    val dateEpochDay: Long? = null,
    val minutesOfDay: Int = 570,
    val people: Int = 2,
    val minPrice: Int = 0,
    val maxPrice: Int = 1_000_000,
) {
    init {
        require(minutesOfDay in 0..1439 && people in 1..10)
        require(minPrice in 0..maxPrice && maxPrice <= 1_000_000)
        require(distanceKm == null || distanceKm > 0)
    }
    val activeCount: Int get() = listOf(regions.isNotEmpty(), distanceKm != null,
        dateEpochDay != null, minPrice != 0 || maxPrice != 1_000_000).count { it }
}

/** Repository supplied search facts. Missing facts must never be treated as a match. */
data class PlaceSearchMetadata(
    val region: String, val distanceKm: Double, val pricePerPerson: Int,
    val capacity: Int, val availableFromDay: Long, val availableThroughDay: Long,
    val opensAtMinute: Int = 0, val closesAtMinute: Int = 1439,
)

fun PlaceFilters.matches(mission: PlaceMission): Boolean {
    if (activeCount == 0) return true
    val facts = mission.searchMetadata ?: return false
    return (regions.isEmpty() || facts.region in regions) &&
        (distanceKm == null || facts.distanceKm <= distanceKm) &&
        facts.pricePerPerson in minPrice..maxPrice &&
        (dateEpochDay == null || (dateEpochDay in facts.availableFromDay..facts.availableThroughDay &&
            minutesOfDay in facts.opensAtMinute..facts.closesAtMinute && people <= facts.capacity))
}
