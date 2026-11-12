package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
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
import com.quare.blitzsplit.core.expenses.domain.usecase.TakePickedReceipt
import com.quare.blitzsplit.core.expenses.domain.usecase.UpdateExpense
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.SaveSplitAsDefault
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.guardUnsavedInput
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveBitcoinInSats
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.feature.splitbill.presentation.model.SPLIT_BILL_SOURCE
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.take
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
    takePickedReceipt: TakePickedReceipt,
    navigator: Navigator,
    observeGroup: ObserveGroup,
    observeExpense: ObserveExpense,
    observeGroupsByActivity: ObserveGroupsByActivity,
    observeExpenses: ObserveExpenses,
    observeSettlements: ObserveSettlements,
    categoryUseCases: BillCategoryUseCases,
    observeBitcoinInSats: ObserveBitcoinInSats,
    connectivityObserver: NetworkConnectivityObserver,
    trackEvent: TrackEvent,
) : TrackedViewModel<SplitBillUiEvent>(trackEvent) {
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

    // Why: "Rachar em qual grupo?" (7.1i), whose "Continuar" moves a new bill to the group picked.
    private val groupPicking = GroupPicking(uiState, route, groupId, trackEvent)

    private val tracking = SplitFormTracking(uiState, trackEvent)
    private val spentAtPicking = SpentAtPicking(uiState, currentTimeMillis, currentTimeZone)

    // Why: "Quem pagou" and "Várias pessoas pagaram".
    private val payerPicking = PayerPicking(uiState, { separator }, trackEvent)

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

    // Why: the duplicate is copied once, as the form opens, so a later sync of the original never overwrites it.
    private var hasFilledDuplicate = false

    // Why: the original's category was already offered to the map once, so a duplicate keeping it doesn't ask again.
    private var duplicatedCategoryId: String? = null

    // Why: Dividir's fifth mode, "Itens da conta" and its items, whose "Pronto" picks the bill's split too.
    private val items = ItemsEditing(
        uiState = uiState,
        separator = { separator },
        receiptItems = { currency -> receipt.findItemsIn(currency) },
        onItemsDone = {
            hasPickedSplit = true
            amountAnchor = null
        },
        trackEvent = trackEvent,
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
        trackEvent = trackEvent,
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
        takePickedReceipt = takePickedReceipt,
        trackEvent = trackEvent,
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
        // Why: a duplicate reads its original once, so one that shows up later never overwrites what is being typed.
        val expenseFlow = route.expenseId?.let { observeExpense(it) }
            ?: route.duplicatesExpenseId?.let { observeExpense(it).take(1) }
            ?: flowOf(null)
        // Why: the form opens once bitcoin's unit is known, so nothing typed or filled in is in the other one first.
        val showsSats = flow { emit(observeBitcoinInSats().first()) }
        combine(group, expenseFlow, showsSats, ::Triple)
            .onEach { (group, expense, sats) ->
                // Why: only as the form opens; later the live unit below rules, which a later sync must not undo.
                if (!hasReadUnit) uiState.update { it.withShowsSats(sats, separator) }
                hasReadUnit = true
                handle(group, expense)
                settledPairs?.refresh(uiState.value)
                // Why: the form is as it opened once filled in; nothing typed after that is unsaved until it differs.
                uiState.update { if (it.isLoaded && it.openedInputs == null) it.copy(openedInputs = it.inputs) else it }
            }.launchIn(viewModelScope)
        navigator.guardUnsavedInput(viewModelScope, route, uiState.map { it.hasUnsavedInput })
        categoryEditing.observe(groupId)
        observeBitcoinInSats()
            .onEach { sats -> uiState.update { it.withShowsSats(sats, separator) } }
            .launchIn(viewModelScope)
        if (settledPairs == null) {
            observeGroupsByActivity()
                .onEach(groupPicking::show)
                .launchIn(viewModelScope)
        } else {
            settledPairs.showIn(uiState)
        }
        navigator.backStack
            .onEach {
                if (isGroupGone) {
                    exits.leaveSettled()
                } else if (isBillGone) {
                    exits.leaveToGroup()
                }
            }.launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: SplitBillUiEvent) {
        handleFormEvent(event)
        settledPairs?.refresh(uiState.value)
    }

    // Why: while it reads a receipt the form takes only what isAllowedWhileReading lets through, so nothing else counts.
    override fun isAccepted(event: SplitBillUiEvent): Boolean =
        !uiState.value.isFormLocked || event.isAllowedWhileReading()

    private fun handleFormEvent(event: SplitBillUiEvent) {
        when (event) {
            is SplitBillUiEvent.OnAmountChanged -> changeAmount(event.text)

            is SplitBillUiEvent.OnCurrencyPicked -> changeCurrency(event.currency)

            SplitBillUiEvent.OnGroupClicked -> groupPicking.open()

            SplitBillUiEvent.OnOpenGroupClicked -> groupPicking.openScreen(exits::openGroup)

            is SplitBillUiEvent.OnGroupPicked -> groupPicking.pick(event.groupId)

            SplitBillUiEvent.OnGroupDone -> groupPicking.confirm()

            SplitBillUiEvent.OnGroupDismissed -> groupPicking.dismiss()

            is SplitBillUiEvent.OnDescriptionChanged -> categoryEditing.changeDescription(event.text)

            is SplitBillUiEvent.Payers -> payerPicking.onEvent(event)

            is SplitBillUiEvent.SpentAt -> spentAtPicking.onEvent(event)

            SplitBillUiEvent.OnSplitClicked -> openSplit()

            SplitBillUiEvent.OnReceiptItemsClicked -> items.openFromReceipt()

            is SplitBillUiEvent.OnSplitModeClicked -> {
                tracking.trackSplitModeSelected(event.mode)
                if (event.mode == SplitMode.ITEMS) {
                    items.openInSplit()
                } else {
                    editSplit { it.toMode(event.mode, separator) }
                }
            }

            is SplitBillUiEvent.OnPersonToggled -> {
                tracking.trackPersonToggled(event.memberId)
                editSplit { it.withPersonToggled(event.memberId) }
            }

            SplitBillUiEvent.OnEveryoneToggled -> {
                tracking.trackEveryoneToggled()
                editSplit { it.withEveryoneToggled() }
            }

            is SplitBillUiEvent.OnPersonValueChanged -> editSplit {
                it.withValueTyped(event.memberId, event.text, separator)
            }

            is SplitBillUiEvent.OnPartsIncreased -> editSplit { it.withPartsChanged(event.memberId, +1) }

            is SplitBillUiEvent.OnPartsDecreased -> editSplit { it.withPartsChanged(event.memberId, -1) }

            SplitBillUiEvent.OnSplitResetToDefault -> uiState.update { it.withSplitEditorReset() }

            SplitBillUiEvent.OnSplitAsDefaultToggled -> {
                tracking.trackSplitAsDefaultToggled()
                uiState.update { it.withSplitAsDefaultToggled() }
            }

            SplitBillUiEvent.OnSplitDone -> confirmSplit()

            SplitBillUiEvent.OnSplitDismissed -> {
                items.forget()
                uiState.update { it.copy(splitEditor = null) }
            }

            is SplitBillUiEvent.Items -> items.onEvent(event)

            is SplitBillUiEvent.Categories -> categoryEditing.onEvent(event)

            is SplitBillUiEvent.Receipt -> receipt.onEvent(event)

            SplitBillUiEvent.OnCategoryMapClicked -> exits.openCategoryMap()

            SplitBillUiEvent.OnSaveClicked -> {
                trackEvent("split_bill_save_clicked", emptyMap())
                save()
            }

            SplitBillUiEvent.OnDeleteClicked -> askToDelete()

            SplitBillUiEvent.OnDeleteConfirmed -> {
                tracking.trackDeleteAnswered(isConfirmed = true)
                delete()
            }

            // Why: the question stays while the bill is being deleted, whatever closes it.
            SplitBillUiEvent.OnDeleteDismissed -> {
                tracking.trackDeleteAnswered(isConfirmed = false)
                uiState.update { if (it.isDeleting) it else it.copy(deletion = null) }
            }

            SplitBillUiEvent.OnCloseClicked -> exits.leave()
        }
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

            route.expenseId == null -> {
                if (expense != null && !hasFilledDuplicate) {
                    // Why: "Duplicar" copies the original once, as the form opens, spent now, its split kept.
                    hasFilledDuplicate = true
                    hasPickedSplit = true
                    duplicatedCategoryId = expense.categoryId
                    fill(group, expense.copy(spentAt = currentTimeMillis()))
                    // Why: now has moved on since the form opened, so the pickers' limit reaches the copy's time.
                    uiState.update { it.withSpentAtLimitRefreshed(currentTimeMillis(), currentTimeZone()) }
                }
                show(group)
            }

            expense == null -> handleBillGone()

            else -> {
                isBillMissing = false
                val previous = editedExpense
                val isFirstRead = previous == null
                if (isFirstRead) {
                    fill(
                        group,
                        expense,
                    )
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
        if (isGroupGone) return exits.leaveSettled()
        uiState.update { it.copy(pickedGroupId = route.groupId) }
        groupId.value = route.groupId
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

    // Why: "Editar conta" and "Duplicar" open filled in with the bill as it was saved, its split as it was typed.
    private fun fill(
        group: Group,
        expense: Expense,
    ) {
        uiState.update { it.toFilledWith(group, expense, separator, currentTimeMillis(), currentTimeZone()) }
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

    private fun changeAmount(text: String) {
        uiState.update { it.withAmountTyped(text, amountAnchor, separator) }
    }

    // Why: the currency beside "Valor" (CurrencyEdits) re-bases the bill as another group's currency does.
    private fun changeCurrency(currency: GroupCurrency) {
        val state = uiState.value
        val group = shownGroup ?: return
        if (!state.canChangeCurrency || currency == state.currency) return
        trackEvent("split_bill_currency_picked", mapOf("currency" to currency.code))
        amountAnchor = null
        items.forget()
        if (state.split.mode == SplitMode.ITEMS) hasPickedSplit = false
        uiState.update { it.withCurrencyPicked(currency, separator) }
        show(group)
        // Why: "Valores" kept their numbers in the new currency, so a new total rescales from them as they are now.
        amountAnchor = uiState.value.valoresAnchorOrNull()
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
        tracking.trackSplitConfirmed(editor)
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
                createExpense.saveNew(state, amountMinor, payers, shares).map { id ->
                    receipt.attachTo(id, state)
                    saveSplitAsDefault.saveOf(state)
                    state.findTeachOfferUnlessKept(id, duplicatedCategoryId)
                }
            } else {
                updateExpense.saveEdit(expenseId, route.groupId, state, amountMinor, payers, shares).map { null }
            }
            result
                .onSuccess { teachOffer ->
                    tracking.trackSaved(
                        state,
                        isNew = expenseId == null,
                        payers = payers.size,
                    )
                    // Why: "Ensinar ao mapa?" (9.3e) only on the group the bill lands on, never later.
                    if (expenseId == null && exits.isLandingOnGroup(state.groupId)) {
                        teachOffer?.let(offerCategoryTeach::invoke)
                    }
                    if (expenseId == null) exits.leaveToBillGroup(state.groupId) else exits.leaveSettled()
                }.onFailure {
                    uiState.update { it.copy(isSaving = false) }
                    uiAction.emit(SplitBillUiAction.ShowSaveError)
                }
        }
    }

    private fun askToDelete() {
        val expense = editedExpense ?: return
        if (!uiState.value.canDelete) return
        trackEvent("bill_delete_clicked", mapOf("source" to SPLIT_BILL_SOURCE))
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
