package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.CreateExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.NoticeBillGone
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.UpdateExpense
import com.quare.blitzsplit.core.groups.domain.model.Group
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
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerSelection
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
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
    private val noticeBillGone: NoticeBillGone,
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

    // Why: the group or the bill went while another screen covered this one, which leaves once it is on top again.
    private var isGroupGone = false
    private var isBillGone = false

    // Why: Room no longer has the bill; if a delete in flight here then fails, someone else's delete won.
    private var isBillMissing = false

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
            .onEach {
                if (isGroupGone) {
                    leave()
                } else if (isBillGone) {
                    leaveToGroup()
                }
            }.launchIn(viewModelScope)
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

            SplitBillUiEvent.OnPayerClicked -> uiState.update {
                it.copy(payerSelection = PayerSelection(memberId = it.payerId.ifEmpty { null }))
            }

            is SplitBillUiEvent.OnPayerPicked -> pickPayer(event.memberId)

            SplitBillUiEvent.OnPayerDone -> uiState.update { it.withPayerConfirmed() }

            SplitBillUiEvent.OnPayerDismissed -> uiState.update { it.copy(payerSelection = null) }

            SplitBillUiEvent.OnSeveralPayersClicked -> uiState.update { it.withSeveralPayersOpened(separator) }

            is SplitBillUiEvent.OnPayerAmountChanged -> changePayerAmount(event.memberId, event.text)

            // Why: only once the amounts add up.
            SplitBillUiEvent.OnSeveralPayersDone -> uiState.update { it.withSeveralPayersConfirmed() }

            SplitBillUiEvent.OnSeveralPayersDismissed -> uiState.update { it.copy(payerAmounts = null) }

            SplitBillUiEvent.OnSpentAtOpened -> refreshSpentAtLimit()

            is SplitBillUiEvent.OnSpentAtChanged -> changeSpentAt(event.epochMillis)

            SplitBillUiEvent.OnSplitClicked -> uiState.update { it.copy(splitEditor = it.split) }

            is SplitBillUiEvent.OnSplitModeClicked -> editSplit { it.toMode(event.mode, separator) }

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

            // Why: the question stays while the bill is being deleted, whatever closes it.
            SplitBillUiEvent.OnDeleteDismissed -> uiState.update { if (it.isDeleting) it else it.copy(deletion = null) }

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
            severalPayers = null,
            payerSelection = null,
            payerAmounts = null,
            spentAt = now,
            spentDay = relativeDayOf(now),
            latestSpentAt = now,
            split = SplitEditor(
                mode = SplitMode.EQUAL,
                rows = emptyList(),
                totalMinor = 0L,
                payerIds = emptyList(),
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
        // Why: a group that comes back before this screen is on top again keeps it open.
        isGroupGone = group == null
        when {
            group == null -> leave()

            route.expenseId == null -> show(group)

            expense == null -> handleBillGone()

            else -> {
                isBillMissing = false
                val previous = editedExpense
                if (previous == null) fill(group, expense) else noticeChange(group, previous, expense)
                editedExpense = expense
                // Why: like Conta, "Apagar esta conta?" says what deleting it changes as the bill is now.
                val meId = group.members.firstOrNull { it.isMe }?.id
                uiState.update { state ->
                    state.copy(deletion = state.deletion?.let { BillDeletion.of(expense, meId) })
                }
                show(group)
            }
        }
    }

    /*
     * Why: a bill deleted here goes back to the group on its own. One someone else deleted closes "Conta" under this
     * screen too and says so on Despesas do grupo; one that never showed leaves without a word.
     */
    private fun handleBillGone() {
        isBillMissing = true
        if (uiState.value.isDeleting || isBillGone) return
        isBillGone = true
        val expenseId = route.expenseId
        if (editedExpense != null && expenseId != null) noticeBillGone(route.groupId, expenseId)
        leaveToGroup()
    }

    // Why: "Editar conta" opens filled in with the bill as it was saved, its split as it was typed.
    private fun fill(
        group: Group,
        expense: Expense,
    ) {
        val people = group.members.map { it.toBillPerson() }
        val payerId = expense.payers
            .singleOrNull()
            ?.memberId
            .orEmpty()
        val severalPayers = expense.payers.takeIf { it.size > 1 }?.let {
            payerAmountsOf(
                expense = expense,
                people = people,
                currency = group.currency,
                decimalSeparator = separator,
            )
        }
        val split = splitEditorOf(
            expense = expense,
            people = people,
            currency = group.currency,
            decimalSeparator = separator,
        )
        amountAnchor = if (expense.splitMode == SplitMode.AMOUNT) expense.amountMinor to split.entries else null
        uiState.update {
            it.copy(
                currency = group.currency,
                amountText = AmountInput.format(
                    amountMinor = expense.amountMinor,
                    currency = group.currency,
                    decimalSeparator = separator,
                ),
                description = expense.description,
                payerId = payerId,
                severalPayers = severalPayers,
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
        val people = group.members.map { it.toBillPerson() }
        // Why: a member who left takes their part of "Valores" along, so a new total rescales only the ones who stay.
        amountAnchor = amountAnchor?.let { (total, entries) ->
            total to entries.filter { entry -> people.any { it.id == entry.memberId } }
        }
        uiState.update { state ->
            /*
             * Why: whoever this device's user is pays by default; someone else's choice stays while they're in the
             * group. A bill being edited keeps its payer even after they left it, so saving never moves who paid.
             * Several payers keep theirs in severalPayers.
             */
            val severalPayers = state.severalPayers?.withPeople(people, group.currency, keepsFormer = state.isEditing)
            val payerId = if (severalPayers != null) {
                ""
            } else {
                state.payerId.takeIf { id -> people.any { it.id == id } || (state.isEditing && id.isNotEmpty()) }
                    ?: people.firstOrNull { it.isMe }?.id
                    ?: people.firstOrNull()?.id.orEmpty()
            }
            val payerIds = severalPayers?.payers?.map { it.memberId } ?: listOf(payerId).filter { it.isNotEmpty() }
            val shown = state.copy(
                groupName = group.name,
                groupInitials = Initials.of(group.name),
                groupColor = group.color,
                currency = group.currency,
                people = people,
                payerId = payerId,
                severalPayers = severalPayers,
                payerSelection = state.payerSelection?.takeIf { selection ->
                    selection.memberId == null || people.any { it.id == selection.memberId }
                },
                payerAmounts = state.payerAmounts?.withPeople(people, group.currency, keepsFormer = state.isEditing),
                split = state.split.withPeople(people, group.currency, payerIds, includesNewcomers = !state.isEditing),
                splitEditor = state.splitEditor?.withPeople(
                    people = people,
                    currency = group.currency,
                    payerIds = payerIds,
                    includesNewcomers = !state.isEditing,
                ),
                isLoaded = true,
            )
            // Why: another currency reads the typed amount in its own minor unit, which the split must add up to.
            shown.withBillTotal()
        }
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
                ).withBillTotal()
        }
    }

    /*
     * Why: the split and the several payers add up to the bill's amount, 0 while the field holds no valid amount.
     * What several people paid stays as typed, so Salvar waits until someone makes it add up again.
     */
    private fun SplitBillUiState.withBillTotal(): SplitBillUiState {
        val total = amountMinor?.takeIf { isAmountValid } ?: 0L
        return copy(
            split = split.withTotal(total, amountAnchor, separator),
            severalPayers = severalPayers?.copy(totalMinor = total),
            payerAmounts = payerAmounts?.copy(totalMinor = total),
        )
    }

    private fun pickPayer(memberId: String) {
        if (uiState.value.people.none { it.id == memberId }) return
        uiState.update { it.copy(payerSelection = PayerSelection(memberId = memberId)) }
    }

    private fun changePayerAmount(
        memberId: String,
        text: String,
    ) {
        val amountText = toAmountText(text)
        uiState.update { state ->
            state.copy(payerAmounts = state.payerAmounts?.withAmountText(memberId, amountText))
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
        val payers = state.severalPayers?.payers ?: listOf(
            ExpensePayer(
                memberId = state.payerId,
                amountMinor = amountMinor,
            ),
        )
        uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val expenseId = route.expenseId
            val result = if (expenseId == null) {
                createExpense(
                    groupId = route.groupId,
                    description = state.description,
                    amountMinor = amountMinor,
                    payers = payers,
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
                    payers = payers,
                    splitMode = state.split.mode,
                    shares = shares,
                    spentAt = state.spentAt,
                )
            }
            result
                .onSuccess {
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
        val meId = uiState.value.people
            .firstOrNull { it.isMe }
            ?.id
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
                    // Why: a sync deleted it meanwhile, so the form would edit a bill that is gone.
                    if (isBillMissing) handleBillGone() else uiAction.emit(SplitBillUiAction.ShowDeleteError)
                }
        }
    }

    // Why: a language whose separator is neither (Arabic's "٫") would fill in text AmountInput can't read back.
    private val separator: Char
        get() = decimalSeparator().takeIf { it in amountSeparators } ?: '.'

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

    private fun relativeDayOf(epochMillis: Long): RelativeDay = SpentDay.relativeDayOf(
        epochMillis = epochMillis,
        nowMillis = currentTimeMillis(),
        timeZone = currentTimeZone(),
    )

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
