package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.BillItemsSplit
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.TypedText
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
    private val onItemsDone: () -> Unit,
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
                ?: emptyItemsOf(people)
            state.copy(
                itemsEditor = ItemsEditor(
                    split = split.toItemsAmong(people, isWholeGroupAtTable = !state.isEditing),
                    people = people,
                    totalMinor = editor.totalMinor,
                    payerIds = editor.payerIds,
                    currency = state.currency,
                ),
            )
        }
    }

    // Why: the bill's split once "Pronto" closes the items, and Dividir under them.
    fun confirm() {
        val state = uiState.value
        val items = state.itemsEditor?.takeIf { it.addsUp } ?: return
        draft = null
        uiState.update {
            it.copy(
                split = items.toSplitEditor(it.split),
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

            SplitBillUiEvent.OnSplitDifferenceClicked -> editItems { items ->
                if (items.canSplitDifference) {
                    items.withSplit { it.copy(extraMinor = it.extraMinor + items.remainingMinor) }
                } else {
                    items
                }
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

            is SplitBillUiEvent.OnItemQuantityChanged -> changeQuantity { AmountInput.parseWhole(event.text) ?: 1L }

            is SplitBillUiEvent.OnItemPriceChanged -> editItem {
                it.withTotal(it.quantity, TypedText.toAmountText(event.text), separator())
            }

            is SplitBillUiEvent.OnItemModeClicked -> editItem { it.toMode(event.mode, separator()) }

            is SplitBillUiEvent.OnItemPersonToggled -> editPerson(
                event.memberId,
            ) { it.copy(isIncluded = !it.isIncluded) }

            is SplitBillUiEvent.OnItemPersonIncreased -> changeCount(event.memberId) { it + 1 }

            is SplitBillUiEvent.OnItemPersonDecreased -> changeCount(event.memberId) { it - 1 }

            is SplitBillUiEvent.OnItemPersonValueChanged -> changeValue(event.memberId, event.text)

            SplitBillUiEvent.OnItemDone -> confirmItem()

            SplitBillUiEvent.OnItemDismissed -> uiState.update { it.copy(itemEditor = null) }

            is SplitBillUiEvent.OnItemDeleteClicked -> uiState.update {
                it.copy(itemDeletion = it.itemsEditor?.deletionOf(event.itemId))
            }

            SplitBillUiEvent.OnItemDeleteConfirmed -> deleteItem()

            SplitBillUiEvent.OnItemDeleteDismissed -> uiState.update { it.copy(itemDeletion = null) }

            is SplitBillUiEvent.OnFeeClicked -> uiState.update {
                it.copy(feeEditor = it.itemsEditor?.feeEditorOf(event.fee, separator()))
            }

            is SplitBillUiEvent.OnFeeValueChanged -> uiState.update { state ->
                val editor = state.feeEditor ?: return@update state
                val text = if (editor.fee == ItemsFee.SERVICE) {
                    TypedText.toPercentText(event.text)
                } else {
                    TypedText.toAmountText(event.text)
                }
                state.copy(feeEditor = editor.copy(valueText = text))
            }

            SplitBillUiEvent.OnFeeDone -> uiState.update { state ->
                val editor = state.feeEditor?.takeIf { it.canSave } ?: return@update state
                state.copy(
                    itemsEditor = state.itemsEditor?.withFee(editor),
                    feeEditor = null,
                )
            }

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
        val mode = uiState.value.itemEditor?.mode ?: return
        when (mode) {
            ItemSplitMode.AMOUNT -> editPerson(memberId) { it.copy(valueText = TypedText.toAmountText(text)) }

            ItemSplitMode.PERCENT -> editPerson(memberId) { it.copy(valueText = TypedText.toPercentText(text)) }

            ItemSplitMode.UNITS, ItemSplitMode.SHARES -> changeCount(memberId) {
                AmountInput.parseWhole(TypedText.toPercentText(text)) ?: 0L
            }

            ItemSplitMode.EQUAL -> Unit
        }
    }
}
