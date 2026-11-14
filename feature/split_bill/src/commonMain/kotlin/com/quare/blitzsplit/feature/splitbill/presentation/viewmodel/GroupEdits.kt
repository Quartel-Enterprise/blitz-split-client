package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.model.TypedText
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroupIncomes
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillGroup
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillIncomes
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.GroupSelection
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsFee
import com.quare.blitzsplit.feature.splitbill.presentation.model.PaidWith
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmounts
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptRequest
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// Why: nothing is drawn until Room is read; a bill is spent now, which is today.
internal fun createInitialBillState(
    route: Route.SplitBill,
    now: Long,
    showsSats: Boolean,
): SplitBillUiState {
    val currency = GroupCurrency.BRL
    return SplitBillUiState(
        isEditing = route.expenseId != null,
        groupId = route.groupId,
        pickedGroupId = route.groupId,
        groupName = "",
        groupInitials = "",
        groupColor = AvatarColor.entries.first(),
        groupPhotoUrl = null,
        groups = emptyList(),
        groupSelection = null,
        currency = currency,
        pickedCurrency = null,
        showsSats = showsSats,
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
        split = createEmptySplit(currency, showsSats),
        splitEditor = null,
        isSplitEditorAsDefaultChecked = false,
        itemsEditor = null,
        itemEditor = null,
        itemDeletion = null,
        feeEditor = null,
        defaultSplit = null,
        defaultSplitExceptionId = null,
        splitAsDefault = null,
        incomes = BillIncomes.None,
        categories = CategoryTree.appOnly,
        categoryId = null,
        isCategoryPicked = false,
        categoryMatches = emptyList(),
        categoryUsage = emptyMap(),
        categoryPicker = null,
        categoryForm = null,
        categoryDeletion = null,
        receipt = null,
        receiptCategoryId = null,
        /*
         * Why: Início's "Ler cupom fiscal" picks the photo first and opens Rachar conta reading it, after "Rachar em
         * qual grupo?" (beta feedback, part 3, item 16). It is taken once it can be read (ReceiptScanning).
         */
        receiptRequest = ReceiptRequest.Picked.takeIf { route.scansReceipt && route.expenseId == null },
        conflict = null,
        settledPair = null,
        deletion = null,
        isOnline = true,
        isSaving = false,
        isDeleting = false,
        isLoaded = false,
        openedInputs = null,
        paidWith = PaidWith(
            methods = emptyList(),
            methodId = null,
            openedMethodId = null,
        ),
    )
}

/*
 * Why: Preferências' "Bitcoin em satoshis" as the form opens, or changed while it is open (the Mac's Ajustes is a
 * window of its own): every amount typed keeps its value in the new unit, as the fields and lists now draw it.
 */
internal fun SplitBillUiState.withShowsSats(
    showsSats: Boolean,
    decimalSeparator: Char,
): SplitBillUiState {
    if (showsSats == this.showsSats) return this
    val showedSats = this.showsSats
    val inUnit = { text: String, currency: GroupCurrency ->
        TypedText.toUnit(text, currency, showedSats, showsSats, decimalSeparator)
    }
    return copy(
        showsSats = showsSats,
        amountText = inUnit(amountText, currency),
        severalPayers = severalPayers?.toUnit(showsSats, inUnit),
        payerAmounts = payerAmounts?.toUnit(showsSats, inUnit),
        split = split.toUnit(showsSats, inUnit),
        splitEditor = splitEditor?.toUnit(showsSats, inUnit),
        itemsEditor = itemsEditor?.copy(showsSats = showsSats),
        itemEditor = itemEditor?.let { editor ->
            val typesAmounts = editor.mode == ItemSplitMode.AMOUNT
            editor.copy(
                showsSats = showsSats,
                unitPriceText = inUnit(editor.unitPriceText, editor.currency),
                rows = editor.rows.map { row ->
                    if (typesAmounts) row.copy(valueText = inUnit(row.valueText, editor.currency)) else row
                },
            )
        },
        feeEditor = feeEditor?.let { editor ->
            val isCouvert = editor.fee == ItemsFee.COUVERT
            editor.copy(
                showsSats = showsSats,
                valueText = if (isCouvert) inUnit(editor.valueText, editor.currency) else editor.valueText,
            )
        },
    )
}

private fun PayerAmounts.toUnit(
    showsSats: Boolean,
    inUnit: (String, GroupCurrency) -> String,
): PayerAmounts = copy(
    showsSats = showsSats,
    rows = rows.map { it.copy(amountText = inUnit(it.amountText, currency)) },
)

private fun SplitEditor.toUnit(
    showsSats: Boolean,
    inUnit: (String, GroupCurrency) -> String,
): SplitEditor {
    val typesAmounts = mode == SplitMode.AMOUNT
    return copy(
        showsSats = showsSats,
        rows = rows.map { row -> if (typesAmounts) row.copy(valueText = inUnit(row.valueText, currency)) else row },
    )
}

/*
 * Why: Início's "Ler cupom fiscal" for a bill whose group splits in a cryptocurrency, once no other group is being
 * picked: the bill opens alone, the photo picked on Início is never read, and neither the choice nor the camera opens
 * later when the bill moves to another group.
 */
internal fun SplitBillUiState.withReceiptRequestSettled(): SplitBillUiState {
    val isAskingNothing = groups.isNotEmpty() && groupSelection == null && !isPickedGroupLoading
    return if (receiptRequest != null && currency.isCrypto && isLoaded &&
        isAskingNothing
    ) {
        copy(receiptRequest = null)
    } else {
        this
    }
}

/*
 * Why: "Continuar" closes the question, the group picked pending until it is read; the same group picked again shows
 * nothing new, so the receipt's request settles here.
 */
internal fun SplitBillUiState.withGroupConfirmed(
    pickedGroupId: String,
    isAccepted: Boolean,
): SplitBillUiState = copy(
    groupSelection = null,
    pickedGroupId = if (isAccepted) pickedGroupId else this.pickedGroupId,
).withReceiptRequestSettled()

/*
 * Why: the split and the several payers add up to the bill's amount, 0 while the field holds no valid amount. What
 * several people paid stays as typed, so Salvar waits until someone makes it add up again.
 */
internal fun SplitBillUiState.withBillTotal(
    amountAnchor: Pair<Long, List<SplitEntry>>?,
    decimalSeparator: Char,
): SplitBillUiState {
    val total = amountMinor?.takeIf { isAmountValid } ?: 0L
    return copy(
        split = split.withTotal(total, amountAnchor, decimalSeparator),
        severalPayers = severalPayers?.copy(totalMinor = total),
        payerAmounts = payerAmounts?.copy(totalMinor = total),
    )
}

// Why: everyone in, equally, until the group's people arrive.
internal fun createEmptySplit(
    currency: GroupCurrency,
    showsSats: Boolean,
): SplitEditor = SplitEditor(
    mode = SplitMode.EQUAL,
    rows = emptyList(),
    totalMinor = 0L,
    payerIds = emptyList(),
    currency = currency,
    showsSats = showsSats,
    items = null,
)

internal fun toBillGroup(group: Group): BillGroup = BillGroup(
    id = group.id,
    name = group.name,
    initials = Initials.of(group.name),
    color = group.color,
    photoUrl = group.photo?.toImageUrl(),
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
    // Why: the bill starts in the new group's currency, not the one picked for the last group.
    pickedCurrency = null,
    payerId = "",
    severalPayers = null,
    payerSelection = null,
    payerAmounts = null,
    split = createEmptySplit(currency, showsSats),
    splitEditor = null,
    itemsEditor = null,
    itemEditor = null,
    itemDeletion = null,
    feeEditor = null,
    defaultSplit = null,
    defaultSplitExceptionId = null,
    splitAsDefault = null,
    // Why: one of the last group's own categories means nothing in another; the app's stay picked.
    categoryId = categoryId?.takeIf { AppCategory.fromKeyOrNull(it) != null },
    isCategoryPicked = isCategoryPicked && AppCategory.fromKeyOrNull(categoryId) != null,
    categoryPicker = null,
    categoryForm = null,
    categoryDeletion = null,
)

/*
 * Why: whoever this device's user is pays by default; someone else's choice stays while they're in the group. A bill
 * being edited keeps its payer even after they left it, so saving never moves who paid. Several payers keep theirs in
 * severalPayers, until every one of them left a new bill.
 */
internal fun SplitBillUiState.withGroupShown(
    group: Group,
    people: List<BillPerson>,
    decimalSeparator: Char,
): SplitBillUiState {
    // Why: the bill's own currency rules, the group's only until one is picked.
    val billCurrency = pickedCurrency ?: group.currency
    val severalPayers = severalPayers
        ?.withPeople(people, billCurrency, showsSats, keepsFormer = isEditing, decimalSeparator)
        ?.takeIf { it.payers.isNotEmpty() }
    val payerId = if (severalPayers != null) {
        ""
    } else {
        payerId.takeIf { id -> people.any { it.id == id } || (isEditing && id.isNotEmpty()) }
            ?: people.firstOrNull { it.isMe }?.id
            ?: people.firstOrNull()?.id.orEmpty()
    }
    return copy(
        groupId = group.id,
        groupName = group.name,
        groupInitials = Initials.of(group.name),
        groupColor = group.color,
        groupPhotoUrl = group.photo?.toImageUrl(),
        currency = billCurrency,
        // Why: moved to another currency, the bill keeps the number typed, not its digits.
        amountText = TypedText.toCurrency(amountText, currency, billCurrency, showsSats, decimalSeparator),
        people = people,
        payerId = payerId,
        severalPayers = severalPayers,
        // Why: the payer of a bill being edited stays marked after they left the group.
        payerSelection = payerSelection?.takeIf { selection ->
            selection.memberId.let { id -> id == null || id == payerId || people.any { it.id == id } }
        },
        payerAmounts = payerAmounts?.withPeople(
            people,
            billCurrency,
            showsSats,
            keepsFormer = isEditing,
            decimalSeparator,
        ),
        isLoaded = true,
    )
}

/*
 * Why: the group's incomes for "By income", read with each group the form shows into the state, so a default by income
 * opens on them at once; in that group's currency. Read per group, so another group's incomes never re-send one gone.
 */
internal fun Flow<Group?>.withIncomesInto(
    state: MutableStateFlow<SplitBillUiState>,
    observeGroupIncomes: ObserveGroupIncomes,
): Flow<Group?> = flatMapLatest { group ->
    val incomes = group?.let { observeGroupIncomes(it.id) } ?: flowOf(emptyMap())
    incomes.map { byMemberId ->
        state.update { it.copy(incomes = BillIncomes(byMemberId, group?.currency ?: it.incomes.currency)) }
        group
    }
}
