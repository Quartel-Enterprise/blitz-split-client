package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillGroup
import com.quare.blitzsplit.feature.splitbill.presentation.model.GroupSelection
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor

// Why: nothing is drawn until Room is read; a bill is spent now, which is today.
internal fun createInitialBillState(
    route: Route.SplitBill,
    now: Long,
): SplitBillUiState {
    val currency = GroupCurrency.BRL
    return SplitBillUiState(
        isEditing = route.expenseId != null,
        groupId = route.groupId,
        groupName = "",
        groupInitials = "",
        groupColor = AvatarColor.entries.first(),
        groups = emptyList(),
        groupSelection = null,
        currency = currency,
        people = emptyList(),
        amountText = "",
        description = "",
        hasEditedAmount = false,
        hasEditedDescription = false,
        payerId = "",
        severalPayers = null,
        payerSelection = null,
        payerAmounts = null,
        spentAt = now,
        spentDay = RelativeDay.TODAY,
        latestSpentAt = now,
        split = createEmptySplit(currency),
        splitEditor = null,
        itemsEditor = null,
        itemEditor = null,
        itemDeletion = null,
        feeEditor = null,
        defaultSplit = null,
        conflict = null,
        settledPair = null,
        deletion = null,
        isOnline = true,
        isSaving = false,
        isDeleting = false,
        isLoaded = false,
    )
}

// Why: everyone in, equally, until the group's people arrive.
internal fun createEmptySplit(currency: GroupCurrency): SplitEditor = SplitEditor(
    mode = SplitMode.EQUAL,
    rows = emptyList(),
    totalMinor = 0L,
    payerIds = emptyList(),
    currency = currency,
    items = null,
)

internal fun toBillGroup(group: Group): BillGroup = BillGroup(
    id = group.id,
    name = group.name,
    initials = Initials.of(group.name),
    color = group.color,
    memberCount = group.members.size,
)

/*
 * Why: the groups "Rachar em qual grupo?" lists, asking with the bill's own marked when asked to. A group that left
 * the device while marked gives the mark back to the bill's group.
 */
internal fun SplitBillUiState.withGroups(
    groups: List<BillGroup>,
    asks: Boolean,
    billGroupId: String,
): SplitBillUiState {
    val selection = if (asks) GroupSelection(groupId = billGroupId) else groupSelection
    return copy(
        groups = groups,
        groupSelection = selection?.let { marked ->
            marked.takeIf { groups.any { it.id == marked.groupId } } ?: GroupSelection(groupId = billGroupId)
        },
    )
}

// Why: only while the sheet is open, and only a group on the device.
internal fun SplitBillUiState.withGroupPicked(groupId: String): SplitBillUiState =
    if (groupSelection == null || groups.none { it.id == groupId }) {
        this
    } else {
        copy(groupSelection = GroupSelection(groupId = groupId))
    }

// Why: another group's people pay and split it from scratch; what it was, how much and when stay.
internal fun SplitBillUiState.toStartInAnotherGroup(): SplitBillUiState = copy(
    payerId = "",
    severalPayers = null,
    payerSelection = null,
    payerAmounts = null,
    split = createEmptySplit(currency),
    splitEditor = null,
    itemsEditor = null,
    itemEditor = null,
    itemDeletion = null,
    feeEditor = null,
    defaultSplit = null,
)
