package com.kiri.feature.record.presentation.bucket

import com.kiri.data.bucket.FakeBucketItemRepository.Companion.item
import com.kiri.domain.model.bucket.BucketItemStatus
import org.junit.Assert.assertEquals
import org.junit.Test

// 같은 미션을 여러 가족이 저장하면 버킷 카드 한 장으로 합쳐 "엄마, 아빠가 저장했어요!" 로 보여준다
class BucketCardGroupsTest {
    // 서버는 최신 저장순으로 준다. item 은 id 가 클수록 나중에 저장한 항목이다
    private val mom = item(1, 1, "엄마").copy(missionId = 100)
    private val dad = item(2, 2, "아빠", deletable = true).copy(missionId = 100)
    private val other = item(3, 1, "엄마").copy(missionId = 200)

    @Test
    fun 같은_미션은_한_카드로_합치고_먼저_저장한_순서로_이름을_잇는다() {
        val cards = listOf(other, dad, mom).mergedByMission()

        assertEquals(listOf(3L, 2L), cards.map { it.item.id })
        assertEquals("엄마, 아빠", cards[1].saverLabel)
        assertEquals("엄마가 저장했어요!", "${withSubjectParticle(cards[0].saverLabel)} 저장했어요!")
        assertEquals("엄마, 아빠가 저장했어요!", "${withSubjectParticle(cards[1].saverLabel)} 저장했어요!")
    }

    @Test
    fun 합친_카드의_대표는_내가_저장한_항목이고_없으면_가장_최근_항목이다() {
        // 내 것(아빠, 지울 수 있음)이 있으면 그것을 대표로 해 삭제·일정 잡기가 내 항목에 적용된다
        assertEquals(2L, listOf(dad, mom).mergedByMission().single().item.id)
        val notMine = dad.copy(deletable = false)
        assertEquals(2L, listOf(notMine, mom).mergedByMission().single().item.id)
    }

    @Test
    fun 상태나_연결된_일정이_다르면_따로_보여준다() {
        val scheduled = mom.copy(status = BucketItemStatus.SCHEDULED, scheduleId = 7)

        val cards = listOf(dad, scheduled).mergedByMission()

        assertEquals(listOf("아빠", "엄마"), cards.map { it.saverLabel })
    }

    @Test
    fun 시트에_쓰는_이름은_그_항목이_들어간_카드의_이름이다() {
        val items = listOf(other, dad, mom)

        assertEquals("엄마, 아빠", items.saverLabelOf(dad))
        assertEquals("엄마", items.saverLabelOf(other))
    }
}
