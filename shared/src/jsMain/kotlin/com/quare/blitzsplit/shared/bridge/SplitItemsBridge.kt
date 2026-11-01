@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.feature.splitbill.presentation.model.FeeEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemDeletion
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsFee
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel

private const val FULL_PERCENT = 100.0

// Why: one line of "Itens da conta"; mode is the ItemSplitMode's code ("equal", "units", "shares", "amount", "percent").
class JsItemsRow(
    val id: String,
    val name: String,
    val quantity: Double,
    val unitPriceMinor: Double,
    val totalMinor: Double,
    val mode: String,
    val consumers: Array<JsBillPerson>,
    val isLoose: Boolean,
)

class JsItemsPersonRow(
    val person: JsBillPerson,
    val itemsMinor: Double,
    val feesMinor: Double,
    val totalMinor: Double,
)

/*
 * Why: ItemsStatus, flat. kind is "loose" (names, isEveryItem, amountMinor the items left out), "left" or "over"
 * (amountMinor, sumMinor, totalMinor, isFromReceipt) or "adds-up" (totalMinor, itemsMinor, serviceMinor, couvertMinor,
 * extraMinor); the other fields are 0.
 */
class JsItemsStatus(
    val kind: String,
    val names: Array<String>,
    val isEveryItem: Boolean,
    val amountMinor: Double,
    val sumMinor: Double,
    val totalMinor: Double,
    val isFromReceipt: Boolean,
    val itemsMinor: Double,
    val serviceMinor: Double,
    val couvertMinor: Double,
    val extraMinor: Double,
)

// Why: "Itens da conta" (7.5), amounts in the currency's minor unit.
class JsItemsEditor(
    val rows: Array<JsItemsRow>,
    val people: Array<JsItemsPersonRow>,
    val totalMinor: Double,
    val itemsMinor: Double,
    val servicePercent: Double,
    val serviceMinor: Double,
    // Why: the couvert per person and the whole table's.
    val couvertMinor: Double,
    val couvertTotalMinor: Double,
    val extraMinor: Double,
    val showsExtra: Boolean,
    val remainingMinor: Double,
    val status: JsItemsStatus,
    val addsUp: Boolean,
    val canAddItem: Boolean,
    val canAddDifferenceItem: Boolean,
    val canSplitDifference: Boolean,
)

class JsItemPersonParts(
    val person: JsBillPerson,
    val parts: Double,
)

/*
 * Why: ItemStatus, flat. kind is "nobody", "units-add-up" (quantity, unitPriceMinor), "units-left" and "units-over"
 * (marked, quantity, count the units missing or over), "each" (amountMinor each, totalMinor, count people), "parts"
 * (count parts, amountMinor a part, severalParts, totalMinor, people), "amount-add-up" (totalMinor), "amount-left"
 * and "amount-over" (amountMinor, assignedMinor, totalMinor), "percent-add-up" (totalMinor, people), "percent-left"
 * and "percent-over" (count the percent missing or over, marked the sum); the other fields are 0.
 */
class JsItemStatus(
    val kind: String,
    val quantity: Double,
    val marked: Double,
    val count: Double,
    val people: Int,
    val unitPriceMinor: Double,
    val amountMinor: Double,
    val assignedMinor: Double,
    val totalMinor: Double,
    val severalParts: Array<JsItemPersonParts>,
)

class JsItemPersonLine(
    val person: JsBillPerson,
    val isIncluded: Boolean,
    val count: Double,
    val valueText: String,
    val shareMinor: Double?,
    val isError: Boolean,
)

// Why: "Item" (7.6); modes are the ItemSplitMode codes it offers, Unidades only for more than one.
class JsItemEditor(
    val itemId: String?,
    val isNew: Boolean,
    val name: String,
    val quantity: Double,
    val unitPriceText: String,
    val totalMinor: Double,
    val mode: String,
    val modes: Array<String>,
    val lines: Array<JsItemPersonLine>,
    val status: JsItemStatus,
    val isStatusError: Boolean,
    // Why: 0 to 100, null in the modes without a bar.
    val progress: Double?,
    val canSave: Boolean,
    val canDecreaseQuantity: Boolean,
    val canIncreaseQuantity: Boolean,
)

class JsItemDeletion(
    val itemId: String,
    val name: String,
    val totalMinor: Double,
    val redoesService: Boolean,
    val leftMinor: Double?,
)

// Why: fee is "service" or "couvert".
class JsFeeEditor(
    val fee: String,
    val valueText: String,
    val canSave: Boolean,
)

// Why: what "Itens da conta" and "Item" send, on the same ViewModel as SplitBillPresenter.
class SplitItemsPresenter internal constructor(
    private val viewModel: () -> SplitBillViewModel,
) {
    fun dismiss() {
        send(SplitBillUiEvent.OnItemsDismissed)
    }

    fun confirm() {
        send(SplitBillUiEvent.OnItemsDone)
    }

    fun addItem() {
        send(SplitBillUiEvent.OnAddItemClicked)
    }

    fun addDifferenceItem() {
        send(SplitBillUiEvent.OnAddDifferenceItemClicked)
    }

    fun splitDifference() {
        send(SplitBillUiEvent.OnSplitDifferenceClicked)
    }

    fun removeDifference() {
        send(SplitBillUiEvent.OnDifferenceRemoved)
    }

    fun openItem(itemId: String) {
        send(SplitBillUiEvent.OnItemClicked(itemId))
    }

    fun changeName(text: String) {
        send(SplitBillUiEvent.OnItemNameChanged(text))
    }

    // Why: each field reads the filtered text back in the same keystroke.
    fun getName(): String = viewModel()
        .uiState.value.itemEditor
        ?.name
        .orEmpty()

    fun increaseQuantity() {
        send(SplitBillUiEvent.OnItemQuantityIncreased)
    }

    fun decreaseQuantity() {
        send(SplitBillUiEvent.OnItemQuantityDecreased)
    }

    fun changeQuantity(text: String) {
        send(SplitBillUiEvent.OnItemQuantityChanged(text))
    }

    fun getQuantity(): String = viewModel()
        .uiState.value.itemEditor
        ?.quantity
        ?.toString()
        .orEmpty()

    fun changePrice(text: String) {
        send(SplitBillUiEvent.OnItemPriceChanged(text))
    }

    fun getPrice(): String = viewModel()
        .uiState.value.itemEditor
        ?.unitPriceText
        .orEmpty()

    fun changeMode(code: String) {
        val mode = ItemSplitMode.fromCodeOrNull(code) ?: return
        send(SplitBillUiEvent.OnItemModeClicked(mode))
    }

    fun togglePerson(memberId: String) {
        send(SplitBillUiEvent.OnItemPersonToggled(memberId))
    }

    fun increasePerson(memberId: String) {
        send(SplitBillUiEvent.OnItemPersonIncreased(memberId))
    }

    fun decreasePerson(memberId: String) {
        send(SplitBillUiEvent.OnItemPersonDecreased(memberId))
    }

    fun changePersonValue(
        memberId: String,
        text: String,
    ) {
        send(SplitBillUiEvent.OnItemPersonValueChanged(memberId, text))
    }

    // Why: Valor's and %'s text, or the count Unidades and Partes keep.
    fun getPersonValue(memberId: String): String {
        val editor = viewModel().uiState.value.itemEditor ?: return ""
        val row = editor.rows.firstOrNull { it.person.id == memberId } ?: return ""
        val isCounted = editor.mode == ItemSplitMode.UNITS || editor.mode == ItemSplitMode.SHARES
        return if (isCounted) countTextOf(row.count) else row.valueText
    }

    fun confirmItem() {
        send(SplitBillUiEvent.OnItemDone)
    }

    fun dismissItem() {
        send(SplitBillUiEvent.OnItemDismissed)
    }

    fun deleteItem(itemId: String) {
        send(SplitBillUiEvent.OnItemDeleteClicked(itemId))
    }

    fun confirmDelete() {
        send(SplitBillUiEvent.OnItemDeleteConfirmed)
    }

    fun dismissDelete() {
        send(SplitBillUiEvent.OnItemDeleteDismissed)
    }

    // Why: "service" or "couvert".
    fun openFee(code: String) {
        val fee = if (code == SERVICE) ItemsFee.SERVICE else ItemsFee.COUVERT
        send(SplitBillUiEvent.OnFeeClicked(fee))
    }

    fun changeFee(text: String) {
        send(SplitBillUiEvent.OnFeeValueChanged(text))
    }

    fun getFee(): String = viewModel()
        .uiState.value.feeEditor
        ?.valueText
        .orEmpty()

    fun confirmFee() {
        send(SplitBillUiEvent.OnFeeDone)
    }

    fun dismissFee() {
        send(SplitBillUiEvent.OnFeeDismissed)
    }

    private fun countTextOf(count: Long): String = if (count > 0) count.toString() else ""

    private fun send(event: SplitBillUiEvent) {
        viewModel().onEvent(event)
    }

    private companion object {
        const val SERVICE = "service"
    }
}

internal fun toJsItemsEditor(editor: ItemsEditor): JsItemsEditor = JsItemsEditor(
    rows = editor.rows
        .map { row ->
            JsItemsRow(
                id = row.item.id,
                name = row.item.name,
                quantity = row.item.quantity.toDouble(),
                unitPriceMinor = row.item.unitPriceMinor.toDouble(),
                totalMinor = row.item.totalMinor.toDouble(),
                mode = row.item.mode.code,
                consumers = row.consumers.map(::toJsBillPerson).toTypedArray(),
                isLoose = row.isLoose,
            )
        }.toTypedArray(),
    people = editor.personRows
        .map {
            JsItemsPersonRow(
                person = toJsBillPerson(it.person),
                itemsMinor = it.itemsMinor.toDouble(),
                feesMinor = it.feesMinor.toDouble(),
                totalMinor = it.totalMinor.toDouble(),
            )
        }.toTypedArray(),
    totalMinor = editor.totalMinor.toDouble(),
    itemsMinor = editor.breakdown.itemsMinor.toDouble(),
    servicePercent = editor.split.servicePercent.toDouble(),
    serviceMinor = editor.breakdown.serviceMinor.toDouble(),
    couvertMinor = editor.split.couvertMinor.toDouble(),
    couvertTotalMinor = editor.breakdown.couvertMinor.toDouble(),
    extraMinor = editor.split.extraMinor.toDouble(),
    showsExtra = editor.showsExtra,
    remainingMinor = editor.remainingMinor.toDouble(),
    status = toJsItemsStatus(editor.status),
    addsUp = editor.addsUp,
    canAddItem = editor.canAddItem,
    canAddDifferenceItem = editor.canAddDifferenceItem,
    canSplitDifference = editor.canSplitDifference,
)

private fun toJsItemsStatus(status: ItemsStatus): JsItemsStatus {
    val loose = status as? ItemsStatus.Loose
    val left = status as? ItemsStatus.AmountLeft
    val over = status as? ItemsStatus.AmountOver
    val addsUp = status as? ItemsStatus.AddsUp
    return JsItemsStatus(
        kind = when (status) {
            is ItemsStatus.Loose -> "loose"
            is ItemsStatus.AmountLeft -> "left"
            is ItemsStatus.AmountOver -> "over"
            is ItemsStatus.AddsUp -> "adds-up"
        },
        names = loose?.items?.map { it.name }?.toTypedArray() ?: emptyArray(),
        isEveryItem = loose?.isEveryItem == true,
        amountMinor = (loose?.looseMinor ?: left?.amountMinor ?: over?.amountMinor ?: 0L).toDouble(),
        sumMinor = (left?.sumMinor ?: over?.sumMinor ?: 0L).toDouble(),
        totalMinor = (left?.totalMinor ?: over?.totalMinor ?: addsUp?.totalMinor ?: 0L).toDouble(),
        isFromReceipt = left?.isFromReceipt ?: over?.isFromReceipt ?: false,
        itemsMinor = (addsUp?.itemsMinor ?: 0L).toDouble(),
        serviceMinor = (addsUp?.serviceMinor ?: 0L).toDouble(),
        couvertMinor = (addsUp?.couvertMinor ?: 0L).toDouble(),
        extraMinor = (addsUp?.extraMinor ?: 0L).toDouble(),
    )
}

internal fun toJsItemEditor(editor: ItemEditor): JsItemEditor = JsItemEditor(
    itemId = editor.itemId,
    isNew = editor.isNew,
    name = editor.name,
    quantity = editor.quantity.toDouble(),
    unitPriceText = editor.unitPriceText,
    totalMinor = editor.totalMinor.toDouble(),
    mode = editor.mode.code,
    modes = editor.modes.map { it.code }.toTypedArray(),
    lines = editor.lines
        .map { line ->
            JsItemPersonLine(
                person = toJsBillPerson(line.row.person),
                isIncluded = line.row.isIncluded,
                count = line.row.count.toDouble(),
                valueText = line.row.valueText,
                shareMinor = line.shareMinor?.toDouble(),
                isError = line.isError,
            )
        }.toTypedArray(),
    status = toJsItemStatus(editor.status),
    isStatusError = editor.isStatusError,
    progress = editor.progress?.let { it * FULL_PERCENT },
    canSave = editor.canSave,
    canDecreaseQuantity = editor.canDecreaseQuantity,
    canIncreaseQuantity = editor.canIncreaseQuantity,
)

private fun toJsItemStatus(status: ItemStatus): JsItemStatus {
    var kind = ""
    var quantity = 0L
    var marked = 0L
    var count = 0L
    var people = 0
    var unitPriceMinor = 0L
    var amountMinor = 0L
    var assignedMinor = 0L
    var totalMinor = 0L
    var severalParts = emptyArray<JsItemPersonParts>()
    when (status) {
        ItemStatus.Nobody -> kind = "nobody"

        is ItemStatus.UnitsAddUp -> {
            kind = "units-add-up"
            quantity = status.quantity
            unitPriceMinor = status.unitPriceMinor
        }

        is ItemStatus.UnitsLeft -> {
            kind = "units-left"
            quantity = status.quantity
            marked = status.marked
            count = status.missing
        }

        is ItemStatus.UnitsOver -> {
            kind = "units-over"
            quantity = status.quantity
            marked = status.marked
            count = status.over
        }

        is ItemStatus.Each -> {
            kind = "each"
            amountMinor = status.eachMinor
            totalMinor = status.totalMinor
            people = status.count
        }

        is ItemStatus.Parts -> {
            kind = "parts"
            count = status.parts
            amountMinor = status.partMinor
            totalMinor = status.totalMinor
            people = status.count
            severalParts = status.severalParts
                .map { JsItemPersonParts(toJsBillPerson(it.person), it.parts.toDouble()) }
                .toTypedArray()
        }

        is ItemStatus.AmountAddsUp -> {
            kind = "amount-add-up"
            totalMinor = status.totalMinor
        }

        is ItemStatus.AmountLeft -> {
            kind = "amount-left"
            amountMinor = status.amountMinor
            assignedMinor = status.assignedMinor
            totalMinor = status.totalMinor
        }

        is ItemStatus.AmountOver -> {
            kind = "amount-over"
            amountMinor = status.amountMinor
            assignedMinor = status.assignedMinor
            totalMinor = status.totalMinor
        }

        is ItemStatus.PercentAddsUp -> {
            kind = "percent-add-up"
            totalMinor = status.totalMinor
            people = status.count
        }

        is ItemStatus.PercentLeft -> {
            kind = "percent-left"
            count = status.percent
            marked = status.sum
        }

        is ItemStatus.PercentOver -> {
            kind = "percent-over"
            count = status.percent
            marked = status.sum
        }
    }
    return JsItemStatus(
        kind = kind,
        quantity = quantity.toDouble(),
        marked = marked.toDouble(),
        count = count.toDouble(),
        people = people,
        unitPriceMinor = unitPriceMinor.toDouble(),
        amountMinor = amountMinor.toDouble(),
        assignedMinor = assignedMinor.toDouble(),
        totalMinor = totalMinor.toDouble(),
        severalParts = severalParts,
    )
}

internal fun toJsItemDeletion(deletion: ItemDeletion): JsItemDeletion = JsItemDeletion(
    itemId = deletion.itemId,
    name = deletion.name,
    totalMinor = deletion.totalMinor.toDouble(),
    redoesService = deletion.redoesService,
    leftMinor = deletion.leftMinor?.toDouble(),
)

internal fun toJsFeeEditor(editor: FeeEditor): JsFeeEditor = JsFeeEditor(
    fee = if (editor.fee == ItemsFee.SERVICE) "service" else "couvert",
    valueText = editor.valueText,
    canSave = editor.canSave,
)
