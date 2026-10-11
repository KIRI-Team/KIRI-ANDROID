package com.kiri.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.notification.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NotificationUiState(
    val loading: Boolean = true,
    val items: List<FamilyNotification> = emptyList(),
    val selected: NotificationCategory = NotificationCategory.ALL,
    val error: String? = null,
    val notice: String? = null,
    val busy: Boolean = false,
) {
    val visibleItems get() = items.filter { selected == NotificationCategory.ALL || it.category == selected }
    val unreadCount get() = items.count { !it.read }
}

@HiltViewModel
class NotificationViewModel @Inject constructor(private val repository: NotificationRepository, private val savedState: SavedStateHandle) : ViewModel() {
    private val _state = MutableStateFlow(NotificationUiState(selected = NotificationCategory.entries.firstOrNull { it.name == savedState.get<String>("notificationCategory") } ?: NotificationCategory.ALL))
    val uiState = _state.asStateFlow()
    private val _target = Channel<FamilyNotification>(Channel.BUFFERED)
    val target = _target.receiveAsFlow()
    private var fetching = false
    init { reload() }
    fun select(category: NotificationCategory) {
        savedState["notificationCategory"] = category.name
        _state.update { it.copy(selected = category, notice = null) }
    }
    fun reload() {
        if (fetching || _state.value.busy) return
        fetching = true
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try { val items = repository.load().sortedByDescending { it.createdAt }; _state.update { it.copy(loading = false, items = items) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(loading = false, error = "알림을 불러오지 못했어요") } }
            finally { fetching = false }
        }
    }
    fun readAll() = mutate {
        repository.markAllRead()
        _state.update { it.copy(items = it.items.map { item -> item.copy(read = true) }) }
    }
    fun open(notification: FamilyNotification) = mutate {
        repository.markRead(notification.id)
        _state.update { it.copy(items = it.items.map { item -> if (item.id == notification.id) item.copy(read = true) else item }) }
        if (!repository.canOpen(notification)) _state.update { it.copy(notice = "더 이상 확인할 수 없는 내용이에요") }
        else _target.send(notification)
    }
    private fun mutate(operation: suspend () -> Unit) {
        if (_state.value.busy || _state.value.loading) return
        _state.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try { operation() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(notice = "읽음 처리하지 못했어요. 다시 시도해주세요") } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
}
