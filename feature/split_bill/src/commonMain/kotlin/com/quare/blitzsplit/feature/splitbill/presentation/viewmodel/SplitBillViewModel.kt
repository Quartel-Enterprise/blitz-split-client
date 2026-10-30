package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.BillSplit
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.DeletedBillNotice
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.CreateExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.UpdateExpense
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillConflict
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitRow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class SplitBillViewModel(
    private val route: Route.SplitBill,
    private val navigator: Navigator,
    private val createExpense: CreateExpense,
    private val updateExpense: UpdateExpense,
    private val deleteExpense: DeleteExpense,
    private val deletedBillNotice: DeletedBillNotice,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    private val decimalSeparator: DeviceDecimalSeparator,
    observeGroup: ObserveGroup,
    observeExpense: ObserveExpense,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<SplitBillUiState>
        field = MutableStateFlow(createInitialState())

    val uiAction: SharedFlow<SplitBillUiAction>
        field = MutableSharedFlow<SplitBillUiAction>()

    val stateWatcher: FlowWatcher<SplitBillUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<SplitBillUiAction> = FlowWatcher(uiAction)

    private val amountSeparators = setOf(',', '.')

    // Why: the group went while another screen covered this one, which then leaves as soon as it is on top again.
    private var isGroupGone = false

    /*
     * Why: the parts of "Valores" as last set in "Dividir", with the total they were set for. A new total rescales
     * from them rather than from the last rescale, so typing "312" through "3" and "31" loses nothing.
     */
    private var amountAnchor: Pair<Long, List<SplitEntry>>? = null

    // Why: the bill being edited as last read, whose version tells someone else's save from the one the form opened.
    private var editedExpense: Expense? = null

    /*
     * Why: a sync may bring other members while the bill is being filled in, or remove the whole group. While editing,
     * it may also bring someone else's edit of the bill, or its deletion.
     */
    init {
        val expenseFlow = route.expenseId?.let { observeExpense(it) } ?: flowOf(null)
        combine(observeGroup(route.groupId), expenseFlow, ::Pair)
            .onEach { (group, expense) -> handle(group, expense) }
            .launchIn(viewModelScope)
        navigator.backStack
            .onEach { if (isGroupGone) leave() }
            .launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SplitBillUiEvent) {
        when (event) {
            is SplitBillUiEvent.OnAmountChanged -> changeAmount(event.text)

            is SplitBillUiEvent.OnDescriptionChanged -> uiState.update {
                it.copy(
                    description = ExpenseRules.toCapped(event.text),
                    hasEditedDescription = true,
                )
            }

            SplitBillUiEvent.OnPayerClicked -> uiState.update { it.copy(payerSelection = it.payerId) }

            is SplitBillUiEvent.OnPayerPicked -> pickPayer(event.memberId)

            SplitBillUiEvent.OnPayerDone -> confirmPayer()

            SplitBillUiEvent.OnPayerDismissed -> uiState.update { it.copy(payerSelection = null) }

            SplitBillUiEvent.OnSpentAtOpened -> refreshSpentAtLimit()

            is SplitBillUiEvent.OnSpentAtChanged -> changeSpentAt(event.epochMillis)

            SplitBillUiEvent.OnSplitClicked -> uiState.update { it.copy(splitEditor = it.split) }

            is SplitBillUiEvent.OnSplitModeClicked -> editSplit { it.toMode(event.mode) }

            is SplitBillUiEvent.OnPersonToggled -> editSplit { editor ->
                editor.withRows { row ->
                    if (row.person.id == event.memberId) row.copy(isIncluded = !row.isIncluded) else row
                }
            }

            SplitBillUiEvent.OnEveryoneToggled -> editSplit { editor ->
                val include = !editor.isEveryoneIncluded
                editor.withRows { it.copy(isIncluded = include) }
            }

            is SplitBillUiEvent.OnPersonValueChanged -> editSplit { editor ->
                val text = valueTextOf(event.text, editor.mode)
                editor.withRows { row -> if (row.person.id == event.memberId) row.copy(valueText = text) else row }
            }

            is SplitBillUiEvent.OnPartsIncreased -> changeParts(event.memberId, +1)

            is SplitBillUiEvent.OnPartsDecreased -> changeParts(event.memberId, -1)

            SplitBillUiEvent.OnSplitDone -> confirmSplit()

            SplitBillUiEvent.OnSplitDismissed -> uiState.update { it.copy(splitEditor = null) }

            SplitBillUiEvent.OnSaveClicked -> save()

            SplitBillUiEvent.OnDeleteClicked -> askToDelete()

            SplitBillUiEvent.OnDeleteConfirmed -> delete()

            SplitBillUiEvent.OnDeleteDismissed -> uiState.update { it.copy(deletion = null) }

            SplitBillUiEvent.OnCloseClicked -> leave()
        }
    }

    private fun createInitialState(): SplitBillUiState {
        val now = currentTimeMillis()
        val currency = GroupCurrency.BRL
        return SplitBillUiState(
            isEditing = route.expenseId != null,
            groupName = "",
            groupInitials = "",
            groupColor = AvatarColor.entries.first(),
            currency = currency,
            people = emptyList(),
            amountText = "",
            description = "",
            hasEditedAmount = false,
            hasEditedDescription = false,
            payerId = "",
            payerSelection = null,
            spentAt = now,
            spentDay = relativeDayOf(now),
            latestSpentAt = now,
            split = SplitEditor(
                mode = SplitMode.EQUAL,
                rows = emptyList(),
                totalMinor = 0L,
                payerId = "",
                currency = currency,
            ),
            splitEditor = null,
            conflict = null,
            deletion = null,
            isOnline = true,
            isSaving = false,
            isDeleting = false,
            isLoaded = false,
        )
    }

    private fun handle(
        group: Group?,
        expense: Expense?,
    ) {
        when {
            group == null -> {
                isGroupGone = true
                leave()
            }

            route.expenseId == null -> show(group)

            expense == null -> handleBillGone()

            else -> {
                val previous = editedExpense
                if (previous == null) fill(group, expense) else noticeChange(group, previous, expense)
                editedExpense = expense
                show(group)
            }
        }
    }

    /*
     * Why: a bill deleted here goes back to the group on its own. One someone else deleted closes "Conta" under this
     * screen too and says so on Despesas do grupo; one that never showed leaves without a word.
     */
    private fun handleBillGone() {
        if (uiState.value.isDeleting) return
        if (editedExpense != null) deletedBillNotice.post(route.groupId)
        leaveToGroup()
    }

    // Why: "Editar conta" opens filled in with the bill as it was saved, its split as it was typed.
    private fun fill(
        group: Group,
        expense: Expense,
    ) {
        val people = group.members.map(::toBillPerson)
        val payerId = expense.payers.firstOrNull()?.memberId.orEmpty()
        val weights = expense.shares.associate { it.memberId to it.weight }
        val rows = people.map { person ->
            val weight = weights[person.id] ?: 0L
            SplitRow(
                person = person,
                isIncluded = weight > 0,
                valueText = when {
                    weight <= 0 || expense.splitMode == SplitMode.EQUAL || expense.splitMode == SplitMode.SHARES -> ""
                    expense.splitMode == SplitMode.AMOUNT -> AmountInput.format(
                        amountMinor = weight,
                        currency = group.currency,
                        decimalSeparator = separator(),
                    )
                    else -> weight.toString()
                },
                parts = if (expense.splitMode == SplitMode.SHARES) weight else 0L,
                shareMinor = null,
            )
        }
        val split = SplitEditor(
            mode = expense.splitMode,
            rows = rows,
            totalMinor = expense.amountMinor,
            payerId = payerId,
            currency = group.currency,
        )
        amountAnchor = if (expense.splitMode == SplitMode.AMOUNT) expense.amountMinor to split.entries else null
        uiState.update {
            it.copy(
                currency = group.currency,
                amountText = AmountInput.format(
                    amountMinor = expense.amountMinor,
                    currency = group.currency,
                    decimalSeparator = separator(),
                ),
                description = expense.description,
                payerId = payerId,
                spentAt = expense.spentAt,
                spentDay = relativeDayOf(expense.spentAt),
                split = split,
            )
        }
    }

    // Why: someone else's save that a sync brought, while the form keeps what is being typed here.
    private fun noticeChange(
        group: Group,
        previous: Expense,
        expense: Expense,
    ) {
        if (expense.updatedAt == previous.updatedAt || expense.isPending) return
        val changer = group.members.firstOrNull { it.id == expense.updatedByMemberId }
        uiState.update {
            it.copy(
                conflict = BillConflict(
                    changerName = changer?.name,
                    changerIsMe = changer?.isMe == true,
                    changedAt = expense.updatedAt,
                ),
            )
        }
    }

    private fun show(group: Group) {
        val people = group.members.map(::toBillPerson)
        // Why: a member who left takes their part of "Valores" along, so a new total rescales only the ones who stay.
        amountAnchor = amountAnchor?.let { (total, entries) ->
            total to entries.filter { entry -> people.any { it.id == entry.memberId } }
        }
        uiState.update { state ->
            // Why: whoever this device's user is pays by default; someone else's choice stays while they're in the group.
            val payerId = state.payerId.takeIf { id -> people.any { it.id == id } }
                ?: people.firstOrNull { it.isMe }?.id
                ?: people.firstOrNull()?.id.orEmpty()
            val shown = state.copy(
                groupName = group.name,
                groupInitials = Initials.of(group.name),
                groupColor = group.color,
                currency = group.currency,
                people = people,
                payerId = payerId,
                payerSelection = state.payerSelection?.takeIf { id -> people.any { it.id == id } },
                split = state.split.withPeople(people, group.currency, payerId),
                splitEditor = state.splitEditor?.withPeople(people, group.currency, payerId),
                isLoaded = true,
            )
            // Why: another currency reads the typed amount in its own minor unit, which the split must add up to.
            shown.withSplitTotal()
        }
    }

    private fun toBillPerson(member: GroupMember): BillPerson = BillPerson(
        id = member.id,
        name = member.name,
        initials = if (member.isMe) Initials.firstOf(member.name) else Initials.of(member.name),
        avatarColor = member.avatarColor,
        isMe = member.isMe,
        hasJoined = member.hasJoined,
    )

    // Why: the people of the group as they are now: a new member comes in "Igual" and stays out of the other modes.
    private fun SplitEditor.withPeople(
        people: List<BillPerson>,
        currency: GroupCurrency,
        payerId: String,
    ): SplitEditor {
        val byId = rows.associateBy { it.person.id }
        val rows = people.map { person ->
            byId[person.id]?.copy(person = person) ?: SplitRow(
                person = person,
                isIncluded = true,
                valueText = "",
                parts = 0L,
                shareMinor = null,
            )
        }
        return copy(
            rows = rows,
            currency = currency,
            payerId = payerId,
        ).withShares()
    }

    // Why: "Hoje" may have become "Ontem" since the screen opened, and the picker may now reach a new day.
    private fun refreshSpentAtLimit() {
        val now = currentTimeMillis()
        uiState.update {
            it.copy(
                spentDay = relativeDayOf(it.spentAt),
                latestSpentAt = now,
            )
        }
    }

    // Why: a later time today, which the pickers can't rule out, becomes now.
    private fun changeSpentAt(epochMillis: Long) {
        val now = currentTimeMillis()
        val spentAt = minOf(epochMillis, now)
        uiState.update {
            it.copy(
                spentAt = spentAt,
                spentDay = relativeDayOf(spentAt),
                latestSpentAt = now,
            )
        }
    }

    private fun changeAmount(text: String) {
        val capped = toAmountText(text)
        uiState.update { state ->
            state
                .copy(
                    amountText = capped,
                    hasEditedAmount = true,
                ).withSplitTotal()
        }
    }

    // Why: the split adds up to the bill's amount, 0 while the field holds no valid amount.
    private fun SplitBillUiState.withSplitTotal(): SplitBillUiState {
        val total = amountMinor?.takeIf { isAmountValid } ?: 0L
        return copy(split = split.withTotal(total))
    }

    // Why: "Mudar o total refaz a divisão por valor proporcionalmente", from the parts last set in "Dividir".
    private fun SplitEditor.withTotal(totalMinor: Long): SplitEditor {
        if (totalMinor == this.totalMinor) return this
        val anchor = amountAnchor
        if (mode != SplitMode.AMOUNT || anchor == null || totalMinor == 0L) {
            return copy(totalMinor = totalMinor).withShares()
        }
        val (anchorTotal, anchorEntries) = anchor
        val scaled = BillSplit
            .calculateRescaledAmounts(
                entries = anchorEntries,
                oldTotalMinor = anchorTotal,
                newTotalMinor = totalMinor,
                payerId = payerId,
            ).associate { it.memberId to it.value }
        return copy(totalMinor = totalMinor)
            .withRows { row ->
                val value = scaled[row.person.id] ?: 0L
                row.copy(valueText = if (value > 0) formatAmount(value) else "")
            }.withShares()
    }

    private fun pickPayer(memberId: String) {
        if (uiState.value.people.none { it.id == memberId }) return
        uiState.update { it.copy(payerSelection = memberId) }
    }

    private fun confirmPayer() {
        val payerId = uiState.value.payerSelection ?: return
        uiState.update { state ->
            state.copy(
                payerId = payerId,
                payerSelection = null,
                split = state.split.copy(payerId = payerId).withShares(),
                splitEditor = state.splitEditor?.copy(payerId = payerId)?.withShares(),
            )
        }
    }

    private fun editSplit(change: (SplitEditor) -> SplitEditor) {
        uiState.update { state ->
            state.copy(splitEditor = state.splitEditor?.let(change)?.withShares())
        }
    }

    private fun changeParts(
        memberId: String,
        delta: Int,
    ) {
        editSplit { editor ->
            editor.withRows { row ->
                if (row.person.id == memberId) row.copy(parts = (row.parts + delta).coerceIn(0L, MAX_PARTS)) else row
            }
        }
    }

    /*
     * Why: a new mode starts from who is in the split now, everyone when nobody is: "Valores" with the equal parts,
     * "%" with 100 shared as evenly as whole numbers allow and "Partes" with one each.
     */
    private fun SplitEditor.toMode(mode: SplitMode): SplitEditor {
        if (mode == this.mode) return this
        val inSplit = entries
            .filter { it.value > 0 }
            .map { it.memberId }
            .toSet()
            .ifEmpty { rows.map { it.person.id }.toSet() }
        val equalEntries = rows.map { row ->
            val isIn = row.person.id in inSplit
            SplitEntry(
                memberId = row.person.id,
                value = if (isIn) 1L else 0L,
            )
        }
        val values: Map<String, Long> = when (mode) {
            SplitMode.EQUAL, SplitMode.SHARES -> equalEntries.associate { it.memberId to it.value }
            SplitMode.AMOUNT -> splitEvenly(totalMinor, equalEntries)
            SplitMode.PERCENT -> splitEvenly(BillSplit.FULL_PERCENT, equalEntries)
        }
        return copy(mode = mode).withRows { row ->
            val value = values[row.person.id] ?: 0L
            row.copy(
                isIncluded = row.person.id in inSplit,
                valueText = when {
                    value <= 0 -> ""
                    mode == SplitMode.AMOUNT -> formatAmount(value)
                    else -> value.toString()
                },
                parts = if (mode == SplitMode.SHARES) value else row.parts,
            )
        }
    }

    private fun SplitEditor.splitEvenly(
        total: Long,
        entries: List<SplitEntry>,
    ): Map<String, Long> = BillSplit
        .split(
            totalMinor = total,
            mode = SplitMode.EQUAL,
            entries = entries,
            payerId = payerId,
        )?.associate { it.memberId to it.amountMinor }
        .orEmpty()

    private fun confirmSplit() {
        val editor = uiState.value.splitEditor?.takeIf { it.addsUp } ?: return
        amountAnchor = if (editor.mode == SplitMode.AMOUNT) editor.totalMinor to editor.entries else null
        uiState.update {
            it.copy(
                split = editor,
                splitEditor = null,
            )
        }
    }

    private fun save() {
        val state = uiState.value
        if (!state.canSave) return
        val amountMinor = state.amountMinor ?: return
        val shares = state.split.shares ?: return
        uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val expenseId = route.expenseId
            val result = if (expenseId == null) {
                createExpense(
                    groupId = route.groupId,
                    description = state.description,
                    amountMinor = amountMinor,
                    payerId = state.payerId,
                    splitMode = state.split.mode,
                    shares = shares,
                    spentAt = state.spentAt,
                ).map { }
            } else {
                updateExpense(
                    expenseId = expenseId,
                    groupId = route.groupId,
                    description = state.description,
                    amountMinor = amountMinor,
                    payerId = state.payerId,
                    splitMode = state.split.mode,
                    shares = shares,
                    spentAt = state.spentAt,
                )
            }
            result.onSuccess {
                leave()
            }.onFailure {
                uiState.update { it.copy(isSaving = false) }
                uiAction.emit(SplitBillUiAction.ShowSaveError)
            }
        }
    }

    private fun askToDelete() {
        val expense = editedExpense ?: return
        if (!uiState.value.canDelete) return
        val meId = uiState.value.people.firstOrNull { it.isMe }?.id
        uiState.update { it.copy(deletion = BillDeletion.of(expense, meId)) }
    }

    private fun delete() {
        val expenseId = route.expenseId ?: return
        val state = uiState.value
        if (state.deletion == null || state.isDeleting) return
        uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            deleteExpense(expenseId)
                .onSuccess { leaveToGroup() }
                .onFailure {
                    uiState.update {
                        it.copy(
                            deletion = null,
                            isDeleting = false,
                        )
                    }
                    uiAction.emit(SplitBillUiAction.ShowDeleteError)
                }
        }
    }

    private fun SplitEditor.withRows(change: (SplitRow) -> SplitRow): SplitEditor = copy(rows = rows.map(change))

    // Why: each row's part, as "Dividir" shows it under or beside the name, while the split adds up.
    private fun SplitEditor.withShares(): SplitEditor {
        val byMember = shares?.associate { it.memberId to it.amountMinor }
        return withRows { row -> row.copy(shareMinor = byMember?.get(row.person.id)) }
    }

    // Why: a language whose separator is neither (Arabic's "٫") would fill in text AmountInput can't read back.
    private fun SplitEditor.formatAmount(amountMinor: Long): String = AmountInput.format(
        amountMinor = amountMinor,
        currency = currency,
        decimalSeparator = separator(),
    )

    private fun separator(): Char = decimalSeparator().takeIf { it in amountSeparators } ?: '.'

    private fun valueTextOf(
        text: String,
        mode: SplitMode,
    ): String = if (mode == SplitMode.AMOUNT) {
        toAmountText(text)
    } else {
        text.filter { it.isDigit() }.take(PERCENT_MAX_LENGTH)
    }

    // Why: what an amount field keeps of what was typed, the bill's and each of Valores'.
    private fun toAmountText(text: String): String =
        text.filter { it.isDigit() || it in amountSeparators }.take(AMOUNT_MAX_LENGTH)

    private fun relativeDayOf(epochMillis: Long): RelativeDay {
        val timeZone = currentTimeZone()
        return SpentDay.relativeDayOf(
            date = SpentDay.dateOf(epochMillis, timeZone),
            nowMillis = currentTimeMillis(),
            timeZone = timeZone,
        )
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }

    // Why: back to Despesas do grupo past "Conta", which would show a bill that is gone.
    private fun leaveToGroup() {
        if (!isOnTop()) return
        val stack = navigator.backStack.value
        val detailIndex = stack.indexOfLast { it is Route.BillDetail && it.expenseId == route.expenseId }
        if (detailIndex > 0) navigator.popTo(detailIndex) else navigator.navigateBack()
    }

    private companion object {
        // Why: "100.000.000,00" with room for the separators someone types.
        const val AMOUNT_MAX_LENGTH = 16
        const val PERCENT_MAX_LENGTH = 3
        const val MAX_PARTS = 99L
    }
}
