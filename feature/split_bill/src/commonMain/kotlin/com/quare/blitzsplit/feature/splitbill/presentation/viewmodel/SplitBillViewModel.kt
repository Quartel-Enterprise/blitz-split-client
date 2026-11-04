package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.model.TypedText
import com.quare.blitzsplit.core.expenses.domain.usecase.AttachBillImage
import com.quare.blitzsplit.core.expenses.domain.usecase.CreateExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.NoticeBillGone
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsByActivity
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettlements
import com.quare.blitzsplit.core.expenses.domain.usecase.OfferCategoryTeach
import com.quare.blitzsplit.core.expenses.domain.usecase.ReadReceipt
import com.quare.blitzsplit.core.expenses.domain.usecase.UpdateExpense
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.SaveSplitAsDefault
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveBitcoinInSats
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.feature.splitbill.presentation.model.GroupSelection
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerSelection
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class SplitBillViewModel(
    private val route: Route.SplitBill,
    private val createExpense: CreateExpense,
    private val updateExpense: UpdateExpense,
    private val deleteExpense: DeleteExpense,
    private val noticeBillGone: NoticeBillGone,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    private val decimalSeparator: DeviceDecimalSeparator,
    private val offerCategoryTeach: OfferCategoryTeach,
    private val saveSplitAsDefault: SaveSplitAsDefault,
    readReceipt: ReadReceipt,
    attachBillImage: AttachBillImage,
    navigator: Navigator,
    observeGroup: ObserveGroup,
    observeExpense: ObserveExpense,
    observeGroupsByActivity: ObserveGroupsByActivity,
    observeExpenses: ObserveExpenses,
    observeSettlements: ObserveSettlements,
    categoryUseCases: BillCategoryUseCases,
    observeBitcoinInSats: ObserveBitcoinInSats,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<SplitBillUiState>
        field = MutableStateFlow(
            createInitialBillState(
                route = route,
                now = currentTimeMillis(),
                showsSats = false,
            ),
        )

    val uiAction: SharedFlow<SplitBillUiAction>
        field = MutableSharedFlow<SplitBillUiAction>()

    val stateWatcher: FlowWatcher<SplitBillUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<SplitBillUiAction> = FlowWatcher(uiAction)

    private val exits = SplitBillExits(navigator, route)

    // Why: the group the bill goes to, which "Rachar em qual grupo?" may change for a new bill.
    private val groupId = MutableStateFlow(route.groupId)

    // Why: the group last shown, so another one picked starts over who paid and how it splits among its own people.
    private var shownGroupId: String? = null

    // Why: the home's "Rachar conta" asks for the group once, as the groups first load; a sync never asks again.
    private var hasAskedForGroup = false

    // Why: the group or the bill went while another screen covered this one, which leaves once it is on top again.
    private var isGroupGone = false
    private var isBillGone = false

    // Why: Room no longer has the bill; if a delete in flight here then fails, someone else's delete won.
    private var isBillMissing = false

    // Why: bitcoin's unit as the form opened was applied; a change after that comes from "Bitcoin em satoshis" itself.
    private var hasReadUnit = false

    /*
     * Why: the parts of "Valores" as last set in "Dividir", with the total they were set for. A new total rescales
     * from them rather than from the last rescale, so typing "312" through "3" and "31" loses nothing.
     */
    private var amountAnchor: Pair<Long, List<SplitEntry>>? = null

    /*
     * Why: a new bill follows the group's "Divisão padrão" (and the people a sync brings) until "Pronto" on Dividir
     * picks its own split; another group picked starts it over from that group's.
     */
    private var hasPickedSplit = false

    // Why: Dividir's fifth mode, "Itens da conta" and its items, whose "Pronto" picks the bill's split too.
    private val items = ItemsEditing(
        uiState = uiState,
        separator = { separator },
        receiptItems = { currency -> receipt.findItemsIn(currency) },
        onItemsDone = {
            hasPickedSplit = true
            amountAnchor = null
        },
    )

    // Why: the group as last shown, which a change of the bill's category shows again with that category's split.
    private var shownGroup: Group? = null

    // Why: "Categoria" (7.7) and the group's own categories (7.8); its exception may change the bill's split.
    private val categoryEditing = CategoryEditing(
        uiState = uiState,
        scope = viewModelScope,
        observeCategoryTree = categoryUseCases.observeCategoryTree,
        observeExpenses = observeExpenses,
        observeCategoryMap = categoryUseCases.observeCategoryMap,
        saveGroupCategory = categoryUseCases.saveGroupCategory,
        deleteGroupCategory = categoryUseCases.deleteGroupCategory,
        emit = { uiAction.emit(it) },
        onCategoryChanged = ::showCategorySplit,
    )

    // Why: "Ler cupom fiscal" (7.1e): what it reads fills the bill, and the category follows it unless the map's words say.
    private val receipt: ReceiptScanning = ReceiptScanning(
        uiState = uiState,
        scope = viewModelScope,
        readReceipt = readReceipt,
        attachBillImage = attachBillImage,
        categoryTerms = { categoryEditing.terms },
        onRead = { read ->
            categoryEditing.rematch {
                it.withReceiptRead(read, separator, currentTimeMillis(), currentTimeZone()).withBillTotal()
            }
        },
        onRemoved = { categoryEditing.rematch { it.copy(receiptCategoryId = null) } },
        onReadStarted = items::forget,
    )

    // Why: the bill being edited as last read, whose version tells someone else's save from the one the form opened.
    private var editedExpense: Expense? = null

    // Why: one read of the bill's group, shared by the form and by what a change to the bill unsettles.
    private val group: SharedFlow<Group?> = groupId
        .flatMapLatest { observeGroup(it) }
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    // Why: the payments a change to the bill unsettles (7.4b), only while editing, since only a saved bill can.
    private val settledPairs = route.expenseId?.let {
        SettledPairs(viewModelScope, it, route.groupId, group, observeExpenses, observeSettlements)
    }

    // Why: what "Apagar esta conta?" says deleting the bill unsettles, read from that one source when it opens.
    private val deletionSettledPair: SettledPairChange?
        get() = settledPairs?.deletionPair?.value

    /*
     * Why: a sync may bring other members while the bill is being filled in, or remove the whole group. While editing,
     * it may also bring someone else's edit of the bill, or its deletion, or a payment it unsettles.
     */
    init {
        val expenseFlow = route.expenseId?.let { observeExpense(it) } ?: flowOf(null)
        // Why: the form opens once bitcoin's unit is known, so nothing typed or filled in is in the other one first.
        val showsSats = flow { emit(observeBitcoinInSats().first()) }
        combine(group, expenseFlow, showsSats, ::Triple)
            .onEach { (group, expense, sats) ->
                // Why: only as the form opens; later the live unit below rules, which a later sync must not undo.
                if (!hasReadUnit) uiState.update { it.withShowsSats(sats, separator) }
                hasReadUnit = true
                handle(group, expense)
                settledPairs?.refresh(uiState.value)
            }.launchIn(viewModelScope)
        categoryEditing.observe(groupId)
        observeBitcoinInSats()
            .onEach { sats -> uiState.update { it.withShowsSats(sats, separator) } }
            .launchIn(viewModelScope)
        if (settledPairs == null) {
            observeGroupsByActivity()
                .onEach(::showGroups)
                .launchIn(viewModelScope)
        } else {
            settledPairs.showIn(uiState)
        }
        navigator.backStack
            .onEach {
                if (isGroupGone) {
                    exits.leave()
                } else if (isBillGone) {
                    exits.leaveToGroup()
                }
            }.launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SplitBillUiEvent) {
        handleEvent(event)
        settledPairs?.refresh(uiState.value)
    }

    private fun handleEvent(event: SplitBillUiEvent) {
        if (uiState.value.isFormLocked && !event.isAllowedWhileReading()) return
        when (event) {
            is SplitBillUiEvent.OnAmountChanged -> changeAmount(event.text)

            SplitBillUiEvent.OnGroupClicked -> uiState.update {
                if (it.canChangeGroup) it.copy(groupSelection = GroupSelection(groupId = it.groupId)) else it
            }

            is SplitBillUiEvent.OnGroupPicked -> uiState.update { it.withGroupPicked(event.groupId) }

            SplitBillUiEvent.OnGroupDone -> confirmGroup()

            SplitBillUiEvent.OnGroupDismissed -> uiState.update {
                it
                    .copy(
                        groupSelection = null,
                    ).withReceiptRequestSettled()
            }

            is SplitBillUiEvent.OnDescriptionChanged -> categoryEditing.changeDescription(event.text)

            SplitBillUiEvent.OnPayerClicked -> uiState.update {
                it.copy(payerSelection = PayerSelection(memberId = it.payerId.ifEmpty { null }))
            }

            is SplitBillUiEvent.OnPayerPicked -> uiState.update { it.withPayerPicked(event.memberId) }

            SplitBillUiEvent.OnPayerDone -> uiState.update { it.withPayerConfirmed() }

            SplitBillUiEvent.OnPayerDismissed -> uiState.update { it.copy(payerSelection = null) }

            SplitBillUiEvent.OnSeveralPayersClicked -> uiState.update { it.withSeveralPayersOpened(separator) }

            is SplitBillUiEvent.OnPayerAmountChanged -> uiState.update {
                it.withPayerAmount(
                    event.memberId,
                    event.text,
                    separator,
                )
            }

            // Why: only once the amounts add up.
            SplitBillUiEvent.OnSeveralPayersDone -> uiState.update { it.withSeveralPayersConfirmed() }

            SplitBillUiEvent.OnSeveralPayersDismissed -> uiState.update { it.copy(payerAmounts = null) }

            SplitBillUiEvent.OnSpentAtOpened -> uiState.update {
                it.withSpentAtLimitRefreshed(currentTimeMillis(), currentTimeZone())
            }

            is SplitBillUiEvent.OnSpentAtChanged -> changeSpentAt(event.epochMillis)

            SplitBillUiEvent.OnSplitClicked -> openSplit()

            SplitBillUiEvent.OnReceiptItemsClicked -> items.openFromReceipt()

            is SplitBillUiEvent.OnSplitModeClicked -> if (event.mode == SplitMode.ITEMS) {
                items.openInSplit()
            } else {
                editSplit { it.toMode(event.mode, separator) }
            }

            is SplitBillUiEvent.OnPersonToggled -> editSplit { editor ->
                editor.withRows { row ->
                    if (row.person.id == event.memberId) row.copy(isIncluded = !row.isIncluded) else row
                }
            }

            SplitBillUiEvent.OnEveryoneToggled -> editSplit { editor ->
                val include = !editor.isEveryoneIncluded
                editor.withRows { it.copy(isIncluded = include) }
            }

            is SplitBillUiEvent.OnPersonValueChanged -> editSplit {
                it.withValueTyped(event.memberId, event.text, separator)
            }

            is SplitBillUiEvent.OnPartsIncreased -> editSplit { it.withPartsChanged(event.memberId, +1) }

            is SplitBillUiEvent.OnPartsDecreased -> editSplit { it.withPartsChanged(event.memberId, -1) }

            SplitBillUiEvent.OnSplitResetToDefault -> uiState.update { it.withSplitEditorReset() }

            SplitBillUiEvent.OnSplitAsDefaultToggled -> uiState.update { it.withSplitAsDefaultToggled() }

            SplitBillUiEvent.OnSplitDone -> confirmSplit()

            SplitBillUiEvent.OnSplitDismissed -> {
                items.forget()
                uiState.update { it.copy(splitEditor = null) }
            }

            is SplitBillUiEvent.Items -> items.onEvent(event)

            is SplitBillUiEvent.Categories -> categoryEditing.onEvent(event)

            is SplitBillUiEvent.Receipt -> receipt.onEvent(event)

            SplitBillUiEvent.OnCategoryMapClicked -> exits.openCategoryMap()

            SplitBillUiEvent.OnSaveClicked -> save()

            SplitBillUiEvent.OnDeleteClicked -> askToDelete()

            SplitBillUiEvent.OnDeleteConfirmed -> delete()

            // Why: the question stays while the bill is being deleted, whatever closes it.
            SplitBillUiEvent.OnDeleteDismissed -> uiState.update { if (it.isDeleting) it else it.copy(deletion = null) }

            SplitBillUiEvent.OnCloseClicked -> exits.leave()
        }
    }

    private fun showGroups(groups: List<Group>) {
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

    private fun confirmGroup() {
        val state = uiState.value
        val picked = state.groupSelection?.groupId ?: return
        uiState.update { it.withGroupConfirmed(picked) }
        if (!state.isSaving && state.groups.any { it.id == picked }) groupId.value = picked
    }

    private fun handle(
        group: Group?,
        expense: Expense?,
    ) {
        // Why: a group that comes back before this screen is on top again keeps it open.
        isGroupGone = group == null
        // Why: a category or bills change arriving before the screen leaves never redraws a deleted group's people.
        if (group == null) shownGroup = null
        when {
            group == null -> handleGroupGone()

            route.expenseId == null -> show(group)

            expense == null -> handleBillGone()

            else -> {
                isBillMissing = false
                val previous = editedExpense
                if (previous ==
                    null
                ) {
                    fill(group, expense)
                } else {
                    uiState.update { it.withChangeNoticed(group, previous, expense) }
                }
                editedExpense = expense
                // Why: like Conta, "Apagar esta conta?" says what deleting it changes as the bill is now.
                val meId = group.members.firstOrNull { it.isMe }?.id
                uiState.update { state ->
                    state.copy(
                        deletion = state.deletion?.let { BillDeletion.of(expense, meId, deletionSettledPair) },
                    )
                }
                show(group)
            }
        }
    }

    // Why: a new bill whose picked group went goes back to the group it was opened for, and closes once that one went.
    private fun handleGroupGone() {
        isGroupGone = route.expenseId != null || groupId.value == route.groupId
        if (isGroupGone) exits.leave() else groupId.value = route.groupId
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
        exits.leaveToGroup()
    }

    // Why: "Editar conta" opens filled in with the bill as it was saved, its split as it was typed.
    private fun fill(
        group: Group,
        expense: Expense,
    ) {
        val spentDay = SpentDay.relativeDayOf(expense.spentAt, currentTimeMillis(), currentTimeZone())
        uiState.update { it.toFilledWith(group, expense, separator, spentDay) }
        val split = uiState.value.split
        amountAnchor = if (split.mode == SplitMode.AMOUNT) expense.amountMinor to split.entries else null
    }

    // Why: a new bill's category picks its split, the exception of its category or the group's default (7.1k).
    private fun showCategorySplit() {
        shownGroup?.let(::show)
    }

    private fun show(group: Group) {
        shownGroup = group
        val people = group.members.map { it.toBillPerson() }
        val isAnotherGroup = shownGroupId != null && shownGroupId != group.id
        shownGroupId = group.id
        if (isAnotherGroup) {
            amountAnchor = null
            hasPickedSplit = false
            items.forget()
        }
        // Why: a member who left takes their part of "Valores" along, so a new total rescales only the ones who stay.
        amountAnchor = amountAnchor?.let { (total, entries) ->
            total to entries.filter { entry -> people.any { it.id == entry.memberId } }
        }
        uiState.update { current ->
            val state = if (isAnotherGroup) current.toStartInAnotherGroup() else current
            val shown = state.withGroupShown(group, people, separator)
            val withSplit = shown.withSplitAmong(
                people = people,
                defaultSplit = group.defaultSplit,
                followsDefault = !state.isEditing && !hasPickedSplit,
                decimalSeparator = separator,
            )
            // Why: another currency converts the typed amount to its own minor unit, which the split must add up to.
            withSplit
                .withItemsAmong(people)
                .withBillTotal()
                .withSplitAsDefaultSettled()
                .withReceiptRequestSettled()
        }
    }

    private fun changeSpentAt(epochMillis: Long) {
        uiState.update { it.withSpentAt(epochMillis, currentTimeMillis(), currentTimeZone()) }
    }

    private fun changeAmount(text: String) {
        uiState.update { state ->
            state
                .copy(
                    amountText = TypedText.toAmountText(
                        state.amountText,
                        text,
                        state.currency,
                        state.showsSats,
                        separator,
                    ),
                    hasEditedAmount = true,
                ).withBillTotal()
        }
    }

    private fun SplitBillUiState.withBillTotal(): SplitBillUiState = withBillTotal(amountAnchor, separator)

    private fun editSplit(change: (SplitEditor) -> SplitEditor) {
        uiState.update { state ->
            state.copy(splitEditor = state.splitEditor?.let(change)?.withShares()).withSplitAsDefaultSettled()
        }
    }

    // Why: a bill split by its items opens Dividir straight on "Itens da conta".
    private fun openSplit() {
        uiState.update { it.withSplitOpened() }
        if (uiState.value.split.mode == SplitMode.ITEMS) items.open()
    }

    private fun confirmSplit() {
        val editor = uiState.value.splitEditor?.takeIf { it.addsUp } ?: return
        if (editor.mode == SplitMode.ITEMS) return
        items.forget()
        hasPickedSplit = true
        amountAnchor = if (editor.mode == SplitMode.AMOUNT) editor.totalMinor to editor.entries else null
        uiState.update { it.withSplitConfirmed(editor) }
    }

    private fun save() {
        val state = uiState.value
        if (!state.canSave) return
        // Why: until the group picked in "Rachar em qual grupo?" is read, who pays and splits are the last group's.
        if (state.groupId != groupId.value) return
        val amountMinor = state.amountMinor ?: return
        val shares = state.split.shares ?: return
        val payers = state.payersToSave
        uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val expenseId = route.expenseId
            val result = if (expenseId == null) {
                createExpense(
                    groupId = state.groupId,
                    description = state.description,
                    amountMinor = amountMinor,
                    payers = payers,
                    splitMode = state.split.mode,
                    shares = shares,
                    items = state.split.items,
                    categoryId = state.categoryIdToSave,
                    spentAt = state.spentAt,
                ).map { id ->
                    receipt.attachTo(id, state)
                    saveSplitAsDefault.saveOf(state)
                    state.teachOfferOrNull(id)
                }
            } else {
                updateExpense(
                    expenseId = expenseId,
                    groupId = route.groupId,
                    description = state.description,
                    amountMinor = amountMinor,
                    payers = payers,
                    splitMode = state.split.mode,
                    shares = shares,
                    items = state.split.items,
                    categoryId = state.categoryIdToSave,
                    spentAt = state.spentAt,
                ).map { null }
            }
            result
                .onSuccess { teachOffer ->
                    // Why: "Ensinar ao mapa?" (9.3e) only on the group the bill lands on, never later.
                    if (expenseId == null && exits.isLandingOnGroup(state.groupId)) {
                        teachOffer?.let(offerCategoryTeach::invoke)
                    }
                    if (expenseId == null) exits.leaveToBillGroup(state.groupId) else exits.leave()
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
        uiState.update { it.copy(deletion = BillDeletion.of(expense, meId, deletionSettledPair)) }
    }

    private fun delete() {
        val expenseId = route.expenseId ?: return
        val state = uiState.value
        if (state.deletion == null || state.isDeleting) return
        uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            deleteExpense(expenseId)
                .onSuccess { exits.leaveToGroup() }
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

    private val separator: Char
        get() = TypedText.amountSeparatorOf(decimalSeparator())
}
