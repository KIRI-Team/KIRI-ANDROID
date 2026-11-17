package com.kiri.data.record

import com.kiri.data.local.MemoryLocalStateStore
import com.kiri.data.local.localStorageScope
import com.kiri.domain.home.DailyQuestion
import com.kiri.domain.home.FamilyHome
import com.kiri.domain.home.FamilyMember
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.record.RecordedQuestion
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 서버에 날짜별 질문 목록이 없어, 받은 오늘의 질문을 계정·가족별로 기기에 모아 기록 캘린더에 보여준다
class QuestionHistoryRepositoryImplTest {

    // 한국 시간 2026-10-13 낮
    private val clock = Clock.fixed(Instant.parse("2026-10-13T03:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 13)
    private val store = MemoryLocalStateStore()

    private class Home(var family: FamilyHome) : HomeRepository {
        val reloaded = mutableListOf<Pair<LocalDate, String?>>()
        var recorded: RecordedQuestion? = null
        override suspend fun load() = family
        override suspend fun saveAnswer(question: DailyQuestion, answer: String) = family
        override suspend fun poke(question: DailyQuestion, memberId: String) = family
        override suspend fun loadRecordedQuestion(date: LocalDate, questionId: String?): RecordedQuestion? {
            reloaded += date to questionId
            return recorded
        }
    }

    private fun family(
        date: LocalDate = today,
        answers: Map<String, String> = mapOf("1" to "좋아", "2" to "나도"),
        members: List<FamilyMember> = listOf(FamilyMember("1", "엄마", "엄마"), FamilyMember("2", "아빠", "아빠")),
        currentUserId: String = "1",
        familyName: String? = "끼리네",
        familyId: String = "10",
    ) = FamilyHome(
        date, currentUserId, familyName, members, DailyQuestion("q-$date", date, "질문 $date"), answers, emptySet(), null,
        serverBacked = true, familyId = familyId,
    )

    private fun repository(home: Home) = QuestionHistoryRepositoryImpl(home, store, clock)

    @Test
    fun 오늘의_질문을_모으고_가족_모두_답했는지_기록한다() = runTest {
        val repo = repository(Home(family()))

        repo.refresh()

        val question = repo.questions.value.getValue(today)
        assertEquals("q-$today", question.questionId)
        assertEquals(mapOf("1" to "엄마", "2" to "아빠"), question.memberNames)
        assertTrue(question.everyoneAnswered)
    }

    @Test
    fun 날이_바뀌어도_지난_질문은_남고_앱을_다시_켜도_기기에서_되살린다() = runTest {
        val home = Home(family(answers = mapOf("1" to "좋아")))
        repository(home).refresh()
        val tomorrow = today.plusDays(1)
        home.family = family(date = tomorrow)
        repository(home).refresh()

        val restored = repository(home)
        restored.refresh()

        assertEquals(setOf(today, tomorrow), restored.questions.value.keys)
        assertFalse(restored.questions.value.getValue(today).everyoneAnswered)
    }

    @Test
    fun 탈퇴한_사람은_지난_질문에서도_탈퇴한_멤버로_보인다() = runTest {
        val home = Home(family())
        val repo = repository(home)
        repo.refresh()

        home.family = family(members = listOf(FamilyMember("1", "엄마", "엄마"), FamilyMember("2", "아빠", "아빠", accessible = false)))
        repo.refresh()

        assertEquals("탈퇴한 멤버", repo.questions.value.getValue(today).memberNames["2"])
    }

    @Test
    fun 다른_계정이나_가족이면_모은_질문을_함께_쓰지_않는다() = runTest {
        val home = Home(family())
        repository(home).refresh()

        home.family = family(familyId = "20", date = today.plusDays(1))
        val other = repository(home)
        other.refresh()

        assertEquals(setOf(today.plusDays(1)), other.questions.value.keys)
    }

    @Test
    fun 둘러보기_중이면_아무것도_모으지_않는다() = runTest {
        val repo = repository(Home(family(familyName = null)))

        repo.refresh()

        assertTrue(repo.questions.value.isEmpty())
    }

    @Test
    fun 모아_둔_날만_질문_id_로_답변을_다시_받는다() = runTest {
        val home = Home(family())
        val repo = repository(home)
        repo.refresh()
        val updated = repo.questions.value.getValue(today).copy(answers = mapOf("1" to "바꾼 답"))
        home.recorded = updated

        // 모아 둔 적 없는 지난 날은 받을 방법이 없다
        repo.refreshQuestion(today.minusDays(3))
        repo.refreshQuestion(today)

        assertEquals(listOf(today to "q-$today"), home.reloaded)
        assertEquals(updated, repo.questions.value.getValue(today))
    }

    @Test
    fun 읽을_수_없는_옛_기록은_버리고_새로_모은다() = runTest {
        val home = Home(family())
        repository(home).refresh()
        val key = "questions/" + home.family.localStorageScope()
        store.write(key, "{깨진 기록")

        val repo = repository(home)
        repo.refresh()

        assertEquals(setOf(today), repo.questions.value.keys)
    }
}
