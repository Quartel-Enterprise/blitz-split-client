package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.BillItemsSplit
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.TypedText
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemPersonRow
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsFee
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

/*
 * Why: "Itens da conta" (7.5) and "Item" (7.6), Dividir's fifth mode, kept out of SplitBillViewModel's own flow. The
 * items typed so far outlive going back to Dividir, so picking Itens again opens them as they were.
 */
internal class ItemsEditing(
    private val uiState: MutableStateFlow<SplitBillUiState>,
    private val separator: () -> Char,
    // Why: a receipt read's items (7.1e) in the bill's currency, which Itens opens with until the bill has its own.
    private val receiptItems: (GroupCurrency) -> BillItems?,
    private val onItemsDone: () -> Unit,
    private val trackEvent: TrackEvent,
) {
    // Why: the items as "Voltar" left them, until another mode's "Pronto" or Dividir closing forgets them.
    private var draft: BillItems? = null

    // Why: Itens in Dividir; for a bill already split by its items, Dividir opens straight on them.
    fun open() {
        uiState.update { state ->
            val editor = state.splitEditor ?: return@update state
            val people = state.people
            val split = draft
                ?: editor.items
                ?: state.split.items
                ?: receiptItems(state.currency)
                ?: emptyItemsOf(people)
            state.copy(
                itemsEditor = ItemsEditor(
                    split = split.toItemsAmong(people, isWholeGroupAtTable = !state.isEditing),
                    people = people,
                    totalMinor = editor.totalMinor,
                    payerIds = editor.payerIds,
                    currency = state.currency,
                    showsSats = state.showsSats,
                ),
            )
        }
    }

    // Why: Itens picked in Dividir, which hides "Usar como padrão do grupo" with the other modes.
    fun openInSplit() {
        open()
        uiState.update { it.withSplitAsDefaultSettled() }
    }

    /*
     * Why: "14 itens" beside "Lido do cupom": Dividir opens under the items, so their back goes to it. Nothing opens
     * once the bill moved to a group of another currency, whose Itens would otherwise open empty.
     */
    fun openFromReceipt() {
        if (receiptItems(uiState.value.currency) == null) return
        trackEvent("receipt_items_clicked", emptyMap())
        uiState.update { it.withSplitOpened() }
        openInSplit()
    }

    // Why: the bill's split once "Pronto" closes the items, and Dividir under them.
    fun confirm() {
        val state = uiState.value
        val items = state.itemsEditor?.takeIf { it.addsUp } ?: return
        trackEvent("bill_items_confirmed", mapOf("items" to items.items.size))
        draft = null
        uiState.update {
            it.copy(
                split = items.toSplitEditor(it.split),
                // Why: a bill split by its items never becomes the group's default.
                splitAsDefault = null,
                splitEditor = null,
                itemsEditor = null,
                itemEditor = null,
                itemDeletion = null,
                feeEditor = null,
            )
        }
        onItemsDone()
    }

    // Why: Dividir closing, or another mode's "Pronto", forgets the items typed there.
    fun forget() {
        draft = null
        uiState.update {
            it.copy(
                itemsEditor = null,
                itemEditor = null,
                itemDeletion = null,
                feeEditor = null,
            )
        }
    }

    fun onEvent(event: SplitBillUiEvent.Items) {
        when (event) {
            SplitBillUiEvent.OnItemsDismissed -> dismiss()

            SplitBillUiEvent.OnItemsDone -> confirm()

            SplitBillUiEvent.OnAddItemClicked -> openItem(
                itemId = null,
                priceMinor = null,
            )

            SplitBillUiEvent.OnAddDifferenceItemClicked -> addDifferenceItem()

            SplitBillUiEvent.OnSplitDifferenceClicked -> {
                if (uiState.value.itemsEditor?.canSplitDifference == true) {
                    trackEvent("bill_items_difference_split_clicked", emptyMap())
                }
                splitDifference()
            }

            SplitBillUiEvent.OnDifferenceRemoved -> editItems { items -> items.withSplit { it.copy(extraMinor = 0L) } }

            is SplitBillUiEvent.OnItemClicked -> openItem(
                itemId = event.itemId,
                priceMinor = null,
            )

            is SplitBillUiEvent.OnItemNameChanged -> editItem {
                it.copy(name = ExpenseRules.toCapped(event.text, BillItemsSplit.NAME_MAX_LENGTH))
            }

            SplitBillUiEvent.OnItemQuantityIncreased -> changeQuantity { it + 1 }

            SplitBillUiEvent.OnItemQuantityDecreased -> changeQuantity { it - 1 }

            // Why: a field emptied to type another number keeps the quantity until a number comes.
            is SplitBillUiEvent.OnItemQuantityChanged -> changeQuantity { AmountInput.parseWhole(event.text) ?: it }

            is SplitBillUiEvent.OnItemPriceChanged -> editItem {
                val unitPriceText = TypedText.toAmountText(
                    it.unitPriceText,
                    event.text,
                    it.currency,
                    it.showsSats,
                    separator(),
                )
                it.withTotal(it.quantity, unitPriceText, separator())
            }

            is SplitBillUiEvent.OnItemModeClicked -> {
                val editor = uiState.value.itemEditor
                if (editor != null && editor.mode != event.mode && event.mode in editor.modes) {
                    trackEvent("bill_item_mode_selected", mapOf("mode" to event.mode))
                }
                editItem { it.toMode(event.mode, separator()) }
            }

            is SplitBillUiEvent.OnItemPersonToggled -> {
                uiState.value.itemEditor?.rows?.firstOrNull { it.person.id == event.memberId }?.let { row ->
                    trackEvent("bill_item_person_toggled", mapOf("is_included" to !row.isIncluded))
                }
                editPerson(event.memberId) { it.copy(isIncluded = !it.isIncluded) }
            }

            is SplitBillUiEvent.OnItemPersonIncreased -> changeCount(event.memberId) { it + 1 }

            is SplitBillUiEvent.OnItemPersonDecreased -> changeCount(event.memberId) { it - 1 }

            is SplitBillUiEvent.OnItemPersonValueChanged -> changeValue(event.memberId, event.text)

            SplitBillUiEvent.OnItemDone -> {
                uiState.value.itemEditor?.takeIf { it.canSave && uiState.value.itemsEditor != null }?.let { editor ->
                    trackEvent("bill_item_confirmed", mapOf("mode" to editor.mode, "is_new" to (editor.itemId == null)))
                }
                confirmItem()
            }

            SplitBillUiEvent.OnItemDismissed -> uiState.update { it.copy(itemEditor = null) }

            // Why: only when its question opens, an item still in the list.
            is SplitBillUiEvent.OnItemDeleteClicked -> {
                uiState.update { it.copy(itemDeletion = it.itemsEditor?.deletionOf(event.itemId)) }
                if (uiState.value.itemDeletion != null) trackEvent("bill_item_delete_clicked", emptyMap())
            }

            // Why: only an answer to a question still open, since Apple's alert also closes itself after its button.
            SplitBillUiEvent.OnItemDeleteConfirmed -> {
                if (uiState.value.itemDeletion != null) trackEvent("bill_item_delete_confirmed", emptyMap())
                deleteItem()
            }

            SplitBillUiEvent.OnItemDeleteDismissed -> {
                if (uiState.value.itemDeletion != null) trackEvent("bill_item_delete_cancelled", emptyMap())
                uiState.update { it.copy(itemDeletion = null) }
            }

            // Why: only when its field opens, over "Itens da conta".
            is SplitBillUiEvent.OnFeeClicked -> {
                uiState.update { it.copy(feeEditor = it.itemsEditor?.feeEditorOf(event.fee, separator())) }
                if (uiState.value.feeEditor != null) trackEvent("bill_items_fee_clicked", mapOf("fee" to event.fee))
            }

            is SplitBillUiEvent.OnFeeValueChanged -> uiState.update { state ->
                val editor = state.feeEditor ?: return@update state
                val text = if (editor.fee == ItemsFee.SERVICE) {
                    TypedText.toPercentText(event.text)
                } else {
                    TypedText.toAmountText(editor.valueText, event.text, editor.currency, editor.showsSats, separator())
                }
                state.copy(feeEditor = editor.copy(valueText = text))
            }

            SplitBillUiEvent.OnFeeDone -> confirmFee()

            SplitBillUiEvent.OnFeeDismissed -> uiState.update { it.copy(feeEditor = null) }
        }
    }

    private fun dismiss() {
        draft = uiState.value.itemsEditor?.split ?: draft
        uiState.update {
            it.copy(
                itemsEditor = null,
                itemEditor = null,
                itemDeletion = null,
                feeEditor = null,
                // Why: a bill split by its items comes back to Dividir by "Valores", each one's total in it.
                splitEditor = it.splitEditor?.toAmountsOfItems(separator()),
            )
        }
    }

    private fun addDifferenceItem() {
        val items = uiState.value.itemsEditor?.takeIf { it.canAddDifferenceItem } ?: return
        openItem(
            itemId = null,
            priceMinor = items.remainingMinor,
        )
    }

    private fun openItem(
        itemId: String?,
        priceMinor: Long?,
    ) {
        uiState.update { state ->
            val items = state.itemsEditor ?: return@update state
            val item = itemId?.let { id -> items.items.firstOrNull { it.id == id } ?: return@update state }
            if (item == null && !items.canAddItem) return@update state
            state.copy(
                itemEditor = itemEditorOf(
                    item = item,
                    people = items.people,
                    payerIds = items.payerIds,
                    currency = items.currency,
                    showsSats = items.showsSats,
                    decimalSeparator = separator(),
                    priceMinor = priceMinor?.takeIf { ExpenseRules.isAmountValid(it, items.currency) },
                ),
            )
        }
    }

    private fun confirmItem() {
        uiState.update { state ->
            val editor = state.itemEditor?.takeIf { it.canSave } ?: return@update state
            val items = state.itemsEditor ?: return@update state
            val item = editor.toItem(id = editor.itemId ?: Uuid.random().toString())
            state.copy(
                itemsEditor = items.withItem(item),
                itemEditor = null,
            )
        }
    }

    private fun deleteItem() {
        uiState.update { state ->
            val deletion = state.itemDeletion ?: return@update state
            state.copy(
                itemsEditor = state.itemsEditor?.toItemsWithout(deletion.itemId),
                itemEditor = state.itemEditor?.takeIf { it.itemId != deletion.itemId },
                itemDeletion = null,
            )
        }
    }

    // Why: a value the field takes goes to the items, its field closing.
    private fun confirmFee() {
        val editor = uiState.value.feeEditor?.takeIf { it.canSave } ?: return
        trackEvent("bill_items_fee_confirmed", mapOf("fee" to editor.fee))
        uiState.update {
            it.copy(
                itemsEditor = it.itemsEditor?.withFee(editor),
                feeEditor = null,
            )
        }
    }

    private fun splitDifference() {
        editItems { items ->
            if (items.canSplitDifference) {
                items.withSplit { it.copy(extraMinor = it.extraMinor + items.remainingMinor) }
            } else {
                items
            }
        }
    }

    private fun editItems(change: (ItemsEditor) -> ItemsEditor) {
        uiState.update { state -> state.copy(itemsEditor = state.itemsEditor?.let(change)) }
    }

    private fun editItem(change: (ItemEditor) -> ItemEditor) {
        uiState.update { state -> state.copy(itemEditor = state.itemEditor?.let(change)) }
    }

    private fun changeQuantity(change: (Long) -> Long) {
        editItem { editor ->
            val quantity = change(editor.quantity).coerceIn(1L, BillItemsSplit.MAX_QUANTITY)
            editor.withTotal(quantity, editor.unitPriceText, separator())
        }
    }

    private fun editPerson(
        memberId: String,
        change: (ItemPersonRow) -> ItemPersonRow,
    ) {
        editItem { editor ->
            if (editor.rows.none { it.person.id == memberId }) return@editItem editor
            editor
                .copy(lastEditedMemberId = memberId)
                .withRows { if (it.person.id == memberId) change(it) else it }
        }
    }

    private fun changeCount(
        memberId: String,
        change: (Long) -> Long,
    ) {
        editPerson(memberId) { row -> row.copy(count = change(row.count).coerceIn(0L, BillItemsSplit.MAX_QUANTITY)) }
    }

    // Why: Valor and % keep the text; Unidades and Partes typed on the web and the Mac become their count.
    private fun changeValue(
        memberId: String,
        text: String,
    ) {
        val editor = uiState.value.itemEditor ?: return
        when (editor.mode) {
            ItemSplitMode.AMOUNT -> editPerson(memberId) {
                it.copy(
                    valueText = TypedText.toAmountText(
                        it.valueText,
                        text,
                        editor.currency,
                        editor.showsSats,
                        separator(),
                    ),
                )
            }

            ItemSplitMode.PERCENT -> editPerson(memberId) { it.copy(valueText = TypedText.toPercentText(text)) }

            ItemSplitMode.UNITS, ItemSplitMode.SHARES -> changeCount(memberId) {
                AmountInput.parseWhole(TypedText.toPercentText(text)) ?: 0L
            }

            ItemSplitMode.EQUAL -> Unit
        }
    }
}
