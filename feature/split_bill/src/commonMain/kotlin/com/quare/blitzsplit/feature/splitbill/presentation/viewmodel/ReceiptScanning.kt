package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTerm
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptCategoryHints
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReadFailure
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptScan
import com.quare.blitzsplit.core.expenses.domain.usecase.AttachBillImage
import com.quare.blitzsplit.core.expenses.domain.usecase.ReadReceipt
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.provider.image.BillImage
import com.quare.blitzsplit.feature.splitbill.presentation.mapper.toReceiptError
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillReceipt
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptPhotoSource
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptRequest
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.SPLIT_BILL_SOURCE
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
    // Why: the person's map terms as the form has them, which the read may choose by with the group's categories.
    private val categoryTerms: () -> List<CategoryTerm>,
    private val onRead: (reading: ReceiptReading) -> Unit,
    private val onRemoved: () -> Unit,
    // Why: the form locks while it reads and its open editors close (`withEditorsClosed`), Itens' draft with them.
    private val onReadStarted: () -> Unit,
    private val trackEvent: TrackEvent,
) {
    // Why: the photo of a receipt read, which the bill attaches as it was read; held here, never in the state.
    private var photo: BillImage? = null

    // Why: the read in flight, which a new photo or removing it cancels so an older answer never fills the bill.
    private var reading: Job? = null

    // Why: the photo the reader didn't answer for, which "Tentar de novo" sends again; held here, never in the state.
    private var unansweredSource: ByteArray? = null

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
            SplitBillUiEvent.OnScanReceiptClicked -> {
                if (uiState.value.canScanReceipt) {
                    trackEvent(
                        "scan_receipt_clicked",
                        mapOf("source" to SPLIT_BILL_SOURCE),
                    )
                }
                uiState.update {
                    if (it.canScanReceipt) {
                        it.copy(receiptRequest = ReceiptRequest.Choice)
                    } else {
                        it
                    }
                }
            }

            /*
             * Why: the gallery and the file chooser hand the photo back after the request ended, so a group turned to a
             * cryptocurrency meanwhile, which no receipt is printed in, drops it. Offline it is still sent, and says so.
             */
            is SplitBillUiEvent.OnReceiptPhotoPicked -> if (!uiState.value.currency.isCrypto) {
                read(event.photo) { trackEvent("photo_picked", mapOf("source" to RECEIPT_SOURCE)) }
            }

            SplitBillUiEvent.OnReceiptRetryClicked -> unansweredSource?.let { source ->
                if (uiState.value.offersReceiptRetry && uiState.value.canScanReceipt) {
                    read(source) { trackEvent("retry_clicked", mapOf("source" to RECEIPT_SOURCE)) }
                }
            }

            is SplitBillUiEvent.OnReceiptSourcePicked -> {
                if (uiState.value.showsReceiptSourceChoice) {
                    trackEvent("receipt_source_picked", mapOf("source" to event.source))
                }
                open(event.source)
            }

            /*
             * Why: a request still waiting for its conditions (the group picked being read) takes it too, so the web's
             * click, which opened the file chooser already, leaves no request behind to open it a second time.
             */
            is SplitBillUiEvent.OnReceiptChoiceSkipped -> uiState.update {
                if (it.isReceiptSourceRequested) it.copy(receiptRequest = ReceiptRequest.Open(event.source)) else it
            }

            SplitBillUiEvent.OnReceiptPickerDismissed -> uiState.update {
                it.copy(receiptRequest = null)
            }

            SplitBillUiEvent.OnReceiptRemoved -> remove()
        }
    }

    // Why: only from the choice still open, so a late tap never opens a picker the form no longer asks for.
    private fun open(source: ReceiptPhotoSource) {
        uiState.update {
            if (it.showsReceiptSourceChoice) it.copy(receiptRequest = ReceiptRequest.Open(source)) else it
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

    // Why: a photo arriving while the bill is edited or saving is dropped; onStarted counts the tap it stands for.
    private fun read(
        source: ByteArray,
        onStarted: () -> Unit = {},
    ) {
        val state = uiState.value
        if (state.isEditing || state.isSaving) return
        onStarted()
        reading?.cancel()
        photo = null
        items = null
        unansweredSource = null
        uiState.update {
            it.withEditorsClosed().copy(
                receiptRequest = null,
                receipt = BillReceipt(
                    status = ReceiptStatus.READING,
                    itemCount = 0,
                    hasPhoto = true,
                    error = null,
                ),
            )
        }
        onReadStarted()
        reading = scope.launch {
            val scan = readReceipt(source, state.currency, ReceiptCategoryHints.of(state.categories, categoryTerms()))
            // Why: a photo nothing was read from may be no receipt at all, so the bill never keeps it (7.1g).
            val scanned = scan as? ReceiptScan.Read
            if (scanned == null) {
                fail((scan as ReceiptScan.Failed).failure, source)
                return@launch
            }
            /*
             * Why: a sync moved the bill to a group of another currency meanwhile. The photo was fine, so it is read
             * again in that currency, and the amounts never land in a minor unit they weren't read in.
             */
            if (uiState.value.currency != state.currency) {
                read(source)
                return@launch
            }
            val read = scanned.reading
            trackEvent("receipt_read", mapOf("result" to "read", "items" to read.items.size))
            photo = scanned.photo
            items = read.toBillItems()
            itemsCurrency = state.currency
            uiState.update {
                it.copy(
                    receipt = BillReceipt(
                        status = ReceiptStatus.READ,
                        itemCount = read.items.size,
                        hasPhoto = true,
                        error = null,
                    ),
                )
            }
            onRead(read)
        }
    }

    private fun fail(
        failure: ReceiptReadFailure,
        source: ByteArray,
    ) {
        val error = failure.toReceiptError()
        trackEvent("receipt_read", mapOf("result" to error, "items" to 0))
        unansweredSource = source.takeIf { error.canRetry }
        uiState.update {
            it.copy(
                receipt = BillReceipt(
                    status = ReceiptStatus.FAILED,
                    itemCount = 0,
                    hasPhoto = false,
                    error = error,
                ),
            )
        }
    }

    private fun remove() {
        reading?.cancel()
        reading = null
        photo = null
        items = null
        unansweredSource = null
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

    private companion object {
        // Why: the photo and its retry, as photo_picked's and retry_clicked's source say, apart from the form's taps.
        const val RECEIPT_SOURCE = "receipt"
    }
}
