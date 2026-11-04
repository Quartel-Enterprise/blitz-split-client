package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.GroupSelection
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/*
 * Why: "Rachar em qual grupo?" (7.1i): the groups a new bill may go to, the question the home's "Rachar conta" asks
 * once, and "Continuar", which moves the bill to the group picked (groupId, which the form reads).
 */
internal class GroupPicking(
    private val uiState: MutableStateFlow<SplitBillUiState>,
    private val route: Route.SplitBill,
    private val groupId: MutableStateFlow<String>,
    private val trackEvent: TrackEvent,
) {
    // Why: the home's "Rachar conta" asks for the group once, as the groups first load; a sync never asks again.
    private var hasAskedForGroup = false

    fun show(groups: List<Group>) {
        val asks = route.picksGroupFirst && !hasAskedForGroup && groups.size > 1
        hasAskedForGroup = true
        uiState.update {
            it
                .withGroups(
                    groups = groups.map(::toBillGroup),
                    asks = asks,
                    billGroupId = groupId.value,
                ).withReceiptRequestSettled()
        }
    }

    fun open() {
        uiState.update {
            if (it.canChangeGroup) it.copy(groupSelection = GroupSelection(groupId = it.groupId)) else it
        }
    }

    fun pick(groupId: String) {
        val before = uiState.value
        uiState.update { it.withGroupPicked(groupId) }
        if (uiState.value != before) trackEvent("split_bill_group_picked", emptyMap())
    }

    fun confirm() {
        val state = uiState.value
        val picked = state.groupSelection?.groupId ?: return
        val isAccepted = !state.isSaving && state.groups.any { it.id == picked }
        uiState.update { it.withGroupConfirmed(picked, isAccepted) }
        if (!isAccepted) return
        trackEvent("split_bill_group_confirmed", mapOf("is_another" to (picked != state.groupId)))
        groupId.value = picked
    }

    fun dismiss() {
        uiState.update { it.copy(groupSelection = null).withReceiptRequestSettled() }
    }
}
