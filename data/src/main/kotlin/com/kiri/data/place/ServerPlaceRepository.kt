package com.kiri.data.place

import com.kiri.data.mission.MissionApi
import com.kiri.data.mission.MissionFeedItem
import com.kiri.data.network.apiResult
import com.kiri.domain.model.mission.MissionBucketState
import com.kiri.domain.model.mission.MissionNotFoundException
import com.kiri.domain.place.*
import com.kiri.domain.repository.BucketItemRepository
import com.kiri.domain.repository.MissionInfoRepository
import com.kiri.domain.model.mission.MissionInfo
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.json.Json

internal class ServerPlaceRepository(
    private val api: MissionApi,
    private val missions: MissionInfoRepository,
    private val buckets: BucketItemRepository,
    private val json: Json,
) : PlaceRepository {
    override suspend fun load(query: PlaceQuery): PlacePage {
        val regions = query.filters.regions.sorted().flatMap { regionCodes.getValue(it) }
            .distinct().ifEmpty { listOf("") }
        val cursorParts = query.cursor?.split(':', limit = 2)
        var index = cursorParts?.first()?.toInt() ?: 0
        var cursor = cursorParts?.getOrNull(1)?.takeIf { it.isNotEmpty() }?.toLong()
        val filters = query.requestFilters()
        val seen = mutableSetOf<Pair<Int, Long?>>()
        while (index < regions.size) {
            check(seen.add(index to cursor)) { "미션 페이지 정보가 반복되어요." }
            val parameters = filters.toMutableMap()
            regions[index].takeIf { it.isNotEmpty() }?.let { parameters["regionCode"] = it }
            cursor?.let { parameters["cursor"] = it.toString() }
            val page = apiResult(json) { api.getFeed(parameters) }.getOrThrow()
            val cards = page.missions.filter { item ->
                (item.price == null || item.price >= query.filters.minPrice) &&
                    (item.title.contains(query.keyword.trim(), ignoreCase = true) ||
                        item.placeName.orEmpty().contains(query.keyword.trim(), ignoreCase = true))
            }.mapNotNull { it.toPlace() }.distinctBy { it.id }
            if (page.hasNext) {
                val next = requireNotNull(page.nextCursor)
                check(next != cursor) { "미션 페이지 정보가 반복되어요." }
                cursor = next
            } else {
                index++
                cursor = null
            }
            if (cards.isNotEmpty()) {
                val next = if (index < regions.size) "$index:${cursor ?: ""}" else null
                return PlacePage(withDetails(cards), totalCount = null, nextCursor = next)
            }
        }
        return PlacePage(emptyList(), totalCount = null)
    }

    override suspend fun save(missionId: String) {
        missions.saveBucket(missionId.toLong()).getOrThrow()
    }

    override suspend fun saved(): List<PlaceMission> = bucketMissionIds().mapNotNull { id ->
        val mission = missions.getMission(id).getOrElse {
            if (it is MissionNotFoundException) return@mapNotNull null
            throw it
        }
        val category = categories[mission.category] ?: return@mapNotNull null
        if (mission.bucketState == MissionBucketState.NONE) return@mapNotNull null
        PlaceMission(id.toString(), mission.title, mission.place?.name.orEmpty(), "", category, canSave = true)
            .withDetail(mission)
    }

    // 피드는 사진·가격·거리만 제공한다. 상세의 평점·버킷 상태를 최대 4건씩 보완한다.
    // 상세 조회 실패가 성공한 목록과 사진까지 지우지 않도록 원본 카드를 유지한다.
    private suspend fun withDetails(cards: List<PlaceMission>): List<PlaceMission> = coroutineScope {
        val semaphore = Semaphore(4)
        cards.map { card ->
            async {
                semaphore.withPermit {
                    val detail = missions.getMission(card.id.toLong()).getOrNull()
                    if (detail != null) card.withDetail(detail)
                    else card
                }
            }
        }.awaitAll()
    }

    private suspend fun bucketMissionIds(ignoreFailure: Boolean = false): Set<Long> {
        val ids = linkedSetOf<Long>()
        val seen = mutableSetOf<Long>()
        var cursor: Long? = null
        do {
            val page = buckets.getBucketItems(cursor = cursor, size = 50).getOrElse {
                if (ignoreFailure) return ids
                throw it
            }
            page.items.forEach { ids.add(it.missionId) }
            if (!page.hasNext) break
            val next = requireNotNull(page.nextCursor)
            check(seen.add(next)) { "버킷 페이지 정보가 반복되어요." }
            cursor = next
        } while (true)
        return ids
    }
}

private fun PlaceMission.withState(state: MissionBucketState) = copy(
    saved = state == MissionBucketState.SAVED,
    previouslyCompleted = state == MissionBucketState.COMPLETED_ONLY,
)

private fun PlaceMission.withDetail(detail: MissionInfo): PlaceMission = withState(detail.bucketState).copy(
    rating = detail.averageRating?.takeIf { it.isFinite() && it in 1.0..5.0 }
        ?.let { String.format(Locale.ROOT, "%.1f", it) },
    imageUrl = imageUrl ?: detail.imageUrl?.takeIf { it.isNotBlank() },
    imageSource = if (imageUrl != null) imageSource else detail.imageSource,
    tags = tags.ifEmpty { listOfNotNull(detail.price?.let { if (it == 0) "무료" else "${it}원" }) },
)

private fun MissionFeedItem.toPlace(): PlaceMission? {
    val category = categories[category] ?: return null
    val tags = listOfNotNull(price?.let { if (it == 0) "무료" else "${it}원" }, distance?.let { "${it}km" })
    return PlaceMission(missionId.toString(), title, placeName.orEmpty(), "", category, tags = tags, canSave = true,
        imageUrl = imageUrl?.takeIf { it.isNotBlank() },
        imageSource = imageSource?.takeIf { !imageUrl.isNullOrBlank() })
}

internal fun PlaceQuery.requestFilters() = buildMap {
    put("size", "12")
    category?.let { category -> put("category", categories.entries.single { it.value == category }.key) }
    filters.distanceKm?.let { put("distance", it.toString()) }
    filters.dateEpochDay?.let {
        put("date", LocalDate.ofEpochDay(it).toString())
        put("time", LocalTime.of(filters.minutesOfDay / 60, filters.minutesOfDay % 60).toString())
    }
    when {
        filters.maxPrice == 0 -> put("isFree", "true")
        filters.maxPrice < 1_000_000 -> put("maxPrice", filters.maxPrice.toString())
    }
}

private val categories = mapOf(
    "EXHIBITION" to PlaceCategory.CULTURE, "POPUP" to PlaceCategory.POPUP,
    "PERFORMANCE" to PlaceCategory.SHOW, "FOOD_CAFE" to PlaceCategory.FOOD,
    "OUTDOOR" to PlaceCategory.OUTDOOR, "EXPERIENCE" to PlaceCategory.EXPERIENCE,
)

private val regionCodes = mapOf(
    "서울" to listOf("11"), "인천" to listOf("28"), "경기" to listOf("41"),
    "대전" to listOf("30"), "세종" to listOf("36"), "충청" to listOf("43", "44"),
    "대구" to listOf("27"), "경상" to listOf("47", "48"), "울산" to listOf("31"),
    "부산" to listOf("26"), "광주" to listOf("29"), "전라" to listOf("52", "46"),
    "강원" to listOf("51"), "제주" to listOf("50"),
)
