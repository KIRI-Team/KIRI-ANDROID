package com.kiri.feature.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.home.*
import com.kiri.domain.mission.*
import com.kiri.domain.record.BucketRepository
import com.kiri.domain.schedule.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class ScheduleUiState(
    val loading: Boolean = true, val busy: Boolean = false, val error: String? = null,
    val family: FamilyHome? = null, val mission: MissionContent? = null,
    val schedule: FamilySchedule? = null, val draft: ScheduleDraft? = null,
    val editing: Boolean = false, val preview: Boolean = false,
    val now: Instant = Instant.EPOCH, val notice: String? = null,
    val editingRevision: Int? = null,
) {
    val canPreview get() = !busy && draft?.let {
        val memoOnly = schedule?.let { old -> it.startsAt == old.startsAt && it.place.trim() == old.place && it.inviteeIds == old.responses.keys - old.proposerId } == true
        (it.startsAt?.isAfter(now) == true || memoOnly) && it.inviteeIds.isNotEmpty() && it.place.isNotBlank() && it.message.length <= 500
    } == true
}

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository, private val buckets: BucketRepository,
    private val home: HomeRepository, private val missions: MissionRepository,
    private val saved: SavedStateHandle, private val clock: Clock,
) : ViewModel() {
    private val id: String? = saved["scheduleId"]
    private val bucketId: String? = saved["bucketId"]
    private val state = MutableStateFlow(ScheduleUiState(now = clock.instant()))
    val uiState = state.asStateFlow()
    private val requestId = saved.get<String>("scheduleRequestId") ?: UUID.randomUUID().toString().also { saved["scheduleRequestId"] = it }

    init {
        viewModelScope.launch { repository.schedules.collect { schedules ->
            val currentId = state.value.schedule?.id ?: id
            schedules.find { it.id == currentId }?.let { schedule -> state.update { it.copy(schedule = schedule, now = clock.instant()) } }
        } }
        reload()
    }
    fun reload() {
        if (state.value.busy) return
        viewModelScope.launch {
            state.update { it.copy(loading = true, error = null) }
            try {
                val family = home.load(); check(!family.browsing) { "가족 연결 후 사용할 수 있어요." }
                buckets.refresh(); repository.refresh()
                val schedule = repository.schedules.value.find { it.id == (state.value.schedule?.id ?: id) }
                val bucket = buckets.buckets.value.find { it.id == bucketId }
                check(schedule != null || bucket != null) { "활동을 찾을 수 없어요." }
                val mission = missions.load(schedule?.missionId ?: requireNotNull(bucket).missionId)
                val initial = schedule?.let { ScheduleDraft(it.bucketId.orEmpty(), it.missionId, it.startsAt, it.responses.keys - it.proposerId, it.message, it.place) }
                    ?: ScheduleDraft(requireNotNull(bucket).id, mission.summary.id, place = mission.summary.place)
                val draft = if (saved.get<Boolean>("hasScheduleDraft") == true) initial.copy(
                    startsAt = saved.get<String>("scheduleStartsAt")?.let(Instant::parse),
                    inviteeIds = saved.get<ArrayList<String>>("scheduleInvitees").orEmpty().toSet(),
                    message = saved["scheduleMessage"] ?: "", place = saved["schedulePlace"] ?: initial.place,
                ) else initial
                state.update { it.copy(loading = false, family = family, mission = mission, schedule = schedule, draft = draft,
                    editing = saved["scheduleEditing"] ?: (schedule == null), preview = saved["schedulePreview"] ?: false, now = clock.instant()) }
                state.update { it.copy(editingRevision = saved["scheduleEditingRevision"] ?: schedule?.revision) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { state.update { it.copy(loading = false, error = e.message ?: "일정을 불러오지 못했어요.") } }
        }
    }
    fun tick() { state.update { it.copy(now = clock.instant()) } }
    fun edit() {
        val s = state.value.schedule ?: return
        if (s.proposerId != state.value.family?.currentUserId || s.cancelled || s.completed) return
        clearDraft()
        state.update { it.copy(editing = true, preview = false, draft = ScheduleDraft(s.bucketId.orEmpty(), s.missionId, s.startsAt, s.responses.keys - s.proposerId, s.message, s.place), error = null) }
        saved["scheduleEditing"] = true
        saved["scheduleEditingRevision"] = s.revision
        state.update { it.copy(editingRevision = s.revision) }
    }
    fun change(draft: ScheduleDraft) {
        if (state.value.busy || !state.value.editing) return
        state.update { it.copy(draft = draft, error = null) }
        saved["hasScheduleDraft"] = true
        saved["scheduleStartsAt"] = draft.startsAt?.toString()
        saved["scheduleInvitees"] = ArrayList(draft.inviteeIds)
        saved["scheduleMessage"] = draft.message
        saved["schedulePlace"] = draft.place
    }
    fun setDateTime(date: LocalDate, time: LocalTime) {
        val draft = state.value.draft ?: return
        change(draft.copy(startsAt = date.atTime(time).atZone(ZoneId.of("Asia/Seoul")).toInstant()))
    }
    fun preview() {
        tick()
        if (!state.value.canPreview) { state.update { it.copy(error = "날짜·시간과 초대할 가족을 확인해주세요.") }; return }
        saved["schedulePreview"] = true
        state.update { it.copy(preview = true, error = null) }
    }
    fun back(): Boolean {
        if (state.value.busy) return true
        if (state.value.preview) { saved["schedulePreview"] = false; state.update { it.copy(preview = false) }; return true }
        if (state.value.editing && state.value.schedule != null) {
            clearDraft(); state.update { it.copy(editing = false, error = null) }; return true
        }
        return false
    }
    fun send(onSent: (FamilySchedule) -> Unit = {}) = mutate {
        val current = state.value; val draft = requireNotNull(current.draft)
        val schedule = current.schedule?.let { repository.update(it.id, draft, current.editingRevision ?: it.revision) } ?: repository.create(draft, requestId)
        clearDraft()
        state.update { it.copy(schedule = schedule, editing = false, preview = false, notice = if (current.schedule == null) "초대장을 보냈어요." else "약속을 수정했어요.") }
        onSent(schedule)
    }
    fun respond(response: MemberResponse) = mutate {
        val s = requireNotNull(state.value.schedule)
        repository.respond(s.id, response, s.revision)
        state.update { it.copy(notice = "${response.label}으로 응답했어요.") }
    }
    fun cancel() = mutate {
        val s = requireNotNull(state.value.schedule)
        repository.cancel(s.id, s.revision)
        state.update { it.copy(notice = "약속을 취소했어요.") }
    }
    private fun clearDraft() {
        listOf("hasScheduleDraft", "scheduleStartsAt", "scheduleInvitees", "scheduleMessage", "schedulePlace", "scheduleEditing", "schedulePreview", "scheduleEditingRevision").forEach { saved.remove<Any>(it) }
    }
    private fun mutate(block: suspend () -> Unit) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { state.update { it.copy(error = e.message ?: "변경하지 못했어요. 입력은 유지했어요.") } }
            finally { state.update { it.copy(busy = false, now = clock.instant()) } }
        }
    }
}
