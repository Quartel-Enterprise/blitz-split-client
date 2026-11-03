package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptCategoryHints
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.core.expenses.domain.usecase.AttachBillImage
import com.quare.blitzsplit.core.expenses.domain.usecase.ReadReceipt
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.provider.image.BillImage
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillReceipt
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/*
 * Why: "Ler cupom fiscal" (7.1e to 7.1g), kept out of SplitBillViewModel's own flow: the photo goes to the reader,
 * what it read fills the bill (onRead), its items wait for "Itens" to open with them, and the photo is attached as the
 * bill's receipt once the bill is saved.
 */
internal class ReceiptScanning(
    private val uiState: MutableStateFlow<SplitBillUiState>,
    private val scope: CoroutineScope,
    private val readReceipt: ReadReceipt,
    private val attachBillImage: AttachBillImage,
    // Why: the group's categories and the person's terms as the form has them, which the read may choose by.
    private val categoryHints: (SplitBillUiState) -> ReceiptCategoryHints,
    private val onRead: (reading: ReceiptReading, beforeRead: SplitBillUiState) -> Unit,
    private val onRemoved: () -> Unit,
) {
    // Why: the photo of a receipt read, which the bill attaches as it was read; held here, never in the state.
    private var photo: BillImage? = null

    // Why: the read in flight, which a new photo or removing it cancels so an older answer never fills the bill.
    private var reading: Job? = null

    /*
     * Why: the receipt's items with nobody marked yet (7.5b), which "Itens" opens with until the bill has its own
     * items; null without a receipt that lists any.
     */
    private var items: BillItems? = null

    // Why: the currency the receipt was read in, whose minor unit its items' prices are in.
    private var itemsCurrency: GroupCurrency? = null

    // Why: none once the bill moved to a group of another currency, whose minor unit would misread the prices.
    fun findItemsIn(currency: GroupCurrency): BillItems? = items?.takeIf { itemsCurrency == currency }

    fun onEvent(event: SplitBillUiEvent.Receipt) {
        when (event) {
            SplitBillUiEvent.OnScanReceiptClicked -> uiState.update {
                if (it.canScanReceipt) it.copy(isReceiptCameraRequested = true) else it
            }

            is SplitBillUiEvent.OnReceiptPhotoPicked -> read(event.photo)

            SplitBillUiEvent.OnReceiptCameraDismissed -> uiState.update { it.copy(isReceiptCameraRequested = false) }

            SplitBillUiEvent.OnReceiptRemoved -> remove()
        }
    }

    // Why: once the bill is saved; a photo the device can't keep leaves the bill as it was saved.
    suspend fun attachTo(
        expenseId: String,
        state: SplitBillUiState,
    ) {
        val image = photo ?: return
        attachBillImage(
            expenseId = expenseId,
            groupId = state.groupId,
            kind = BillAttachmentKind.RECEIPT,
            image = image,
            addedByMemberId = state.people.firstOrNull { it.isMe }?.id,
        )
    }

    private fun read(source: ByteArray) {
        val state = uiState.value
        if (state.isEditing || state.isSaving) return
        reading?.cancel()
        photo = null
        items = null
        uiState.update {
            it.copy(
                isReceiptCameraRequested = false,
                receipt = BillReceipt(
                    status = ReceiptStatus.READING,
                    itemCount = 0,
                    hasPhoto = true,
                ),
            )
        }
        reading = scope.launch {
            val scan = readReceipt(source, state.currency, categoryHints(state))
            /*
             * Why: a photo nothing was read from may be no receipt at all, so the bill never keeps it (7.1g). A sync
             * that moved the bill to a group of another currency meanwhile reads the same, so the amounts never land
             * in a minor unit they weren't read in.
             */
            val read = scan.reading?.takeIf { uiState.value.currency == state.currency }
            if (read == null) {
                uiState.update {
                    it.copy(
                        receipt = BillReceipt(
                            status = ReceiptStatus.UNREADABLE,
                            itemCount = 0,
                            hasPhoto = false,
                        ),
                    )
                }
                return@launch
            }
            photo = scan.photo
            items = read.toBillItems()
            itemsCurrency = state.currency
            uiState.update {
                it.copy(
                    receipt = BillReceipt(
                        status = ReceiptStatus.READ,
                        itemCount = read.items.size,
                        hasPhoto = true,
                    ),
                )
            }
            onRead(read, state)
        }
    }

    private fun remove() {
        reading?.cancel()
        reading = null
        photo = null
        items = null
        uiState.update { it.copy(receipt = null) }
        onRemoved()
    }

    // Why: nobody marked in any item (7.5b); the table is set when "Itens" opens, among the group's people then.
    private fun ReceiptReading.toBillItems(): BillItems? {
        if (items.isEmpty()) return null
        return BillItems(
            items = items.map { item ->
                BillItem(
                    id = Uuid.random().toString(),
                    name = item.name,
                    quantity = item.quantity,
                    unitPriceMinor = item.unitPriceMinor,
                    mode = ItemSplitMode.EQUAL,
                    entries = emptyList(),
                )
            },
            servicePercent = servicePercent,
            couvertMinor = 0L,
            couvertMemberIds = emptyList(),
            extraMinor = 0L,
            isFromReceipt = true,
        )
    }
}
