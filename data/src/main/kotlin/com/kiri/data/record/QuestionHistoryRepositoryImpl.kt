package com.kiri.data.record

import com.kiri.data.local.LocalStateStore
import com.kiri.data.local.localStorageScope
import com.kiri.domain.home.repository.FamilyHome
import com.kiri.domain.home.repository.HomeRepository
import com.kiri.domain.question.repository.QuestionHistoryRepository
import com.kiri.domain.question.model.RecordedQuestion
import com.kiri.domain.common.kiriToday
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// 서버에 날짜별 질문 목록 API 가 없어, 받은 오늘의 질문을 계정·가족별로 기기에 모아 둔다.
// 다른 계정이나 가족으로 바뀌면 다른 저장 공간을 쓰고, 둘러보기 중이면 아무것도 보여주지 않는다
@Singleton
internal class QuestionHistoryRepositoryImpl @Inject constructor(
    private val home: HomeRepository,
    private val store: LocalStateStore,
    private val clock: Clock,
) : QuestionHistoryRepository {
    private val mutex = Mutex()
    private val state = MutableStateFlow<Map<LocalDate, RecordedQuestion>>(emptyMap())
    override val questions: StateFlow<Map<LocalDate, RecordedQuestion>> = state.asStateFlow()

    // 지금 보여주는 기록의 저장 공간. 계정·가족이 바뀌면 다시 읽는다
    private var scope: String? = null

    override suspend fun refresh() = mutex.withLock {
        val family = family() ?: return@withLock
        val today = family.question?.let { question ->
            val members = family.members.filter { it.accessible }
            RecordedQuestion(
                date = question.date,
                text = question.text,
                answers = family.answers,
                memberNames = family.memberNames(),
                everyoneAnswered = members.isNotEmpty() && members.all { it.id in family.answers },
                questionId = question.id,
            )
        }
        // 지난 질문의 이름도 지금 닉네임으로 맞춘다 (탈퇴한 사람은 "탈퇴한 멤버")
        val renamed = (state.value + listOfNotNull(today).associateBy { it.date }).mapValues { (_, question) ->
            question.copy(memberNames = question.memberNames.mapValues { (id, _) -> family.nameOf(id) })
        }
        publish(renamed)
    }

    override suspend fun refreshQuestion(date: LocalDate) = mutex.withLock {
        family() ?: return@withLock
        val existing = state.value[date]
        // 지난 날은 모아 둔 질문 id 가 있어야 다시 받을 수 있다
        if (date != clock.kiriToday() && existing?.questionId == null) return@withLock
        val fresh = home.loadRecordedQuestion(date, existing?.questionId) ?: return@withLock
        if (fresh != existing) publish(state.value + (date to fresh))
    }

    // 가족 정보를 받아 저장 공간을 맞춘다. 둘러보기 중이거나 저장할 수 없으면 기록을 비우고 null
    private suspend fun family(): FamilyHome? {
        val family = try {
            home.family()
        } catch (e: Exception) {
            clear()
            throw e
        }
        val next = family.localStorageScope()
        if (next == null) {
            clear()
            return null
        }
        if (next != scope) {
            scope = next
            state.value = store.read(key(next))?.let { decode(it) }.orEmpty()
        }
        return family
    }

    private fun clear() {
        scope = null
        state.value = emptyMap()
    }

    private suspend fun publish(value: Map<LocalDate, RecordedQuestion>) {
        if (value == state.value) return
        store.write(key(requireNotNull(scope)), encode(value))
        state.value = value
    }

    private fun FamilyHome.memberNames() = members.associate { it.id to nameOf(it.id) }

    private fun FamilyHome.nameOf(id: String) =
        members.find { it.id == id && it.accessible }?.nickname ?: "탈퇴한 멤버"

    private fun key(scope: String) = "questions/$scope"

    private fun encode(value: Map<LocalDate, RecordedQuestion>): String =
        json.encodeToString(StoredQuestions.serializer(), StoredQuestions(value.values.map { it.toStored() }))

    // 읽지 못하는 기록(형식이 바뀐 옛 기록 등)은 버리고 처음부터 다시 모은다
    private fun decode(raw: String): Map<LocalDate, RecordedQuestion>? = try {
        json.decodeFromString(StoredQuestions.serializer(), raw).questions
            .map { it.toQuestion() }
            .associateBy { it.date }
    } catch (e: Exception) {
        null
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class StoredQuestions(val questions: List<StoredQuestion>)

@Serializable
private data class StoredQuestion(
    val date: String,
    val text: String,
    val answers: Map<String, String>,
    val names: Map<String, String>,
    val everyoneAnswered: Boolean,
    val questionId: String? = null,
)

private fun RecordedQuestion.toStored() =
    StoredQuestion(date.toString(), text, answers, memberNames, everyoneAnswered, questionId)

private fun StoredQuestion.toQuestion() =
    RecordedQuestion(LocalDate.parse(date), text, answers, names, everyoneAnswered, questionId)

@Module
@InstallIn(SingletonComponent::class)
internal abstract class QuestionHistoryModule {
    @Binds
    abstract fun bindQuestionHistoryRepository(impl: QuestionHistoryRepositoryImpl): QuestionHistoryRepository
}
