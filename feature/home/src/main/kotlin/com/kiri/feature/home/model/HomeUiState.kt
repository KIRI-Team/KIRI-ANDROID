package com.kiri.feature.home.model

import com.kiri.domain.home.*

data class HomeUiState(
    val loading: Boolean = true,
    val home: FamilyHome? = null,
    val loadError: Boolean = false,
    val selectedMemberId: String? = null,
    val editing: Boolean = false,
    val draft: String = "",
    val draftQuestionId: String? = null,
    val showingMission: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val answerSaved: Boolean = false,
) {
    val canSave: Boolean get() = editing && draft.isNotBlank() && !busy && home?.question != null
}
