package com.quare.blitzsplit.android.storescreenshots

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.quare.blitzsplit.android.designsystem.layout.previewInitialsOf
import com.quare.blitzsplit.android.feature.splitbill.ItemsEditorContent
import com.quare.blitzsplit.android.feature.splitbill.SplitBillContent
import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillGroup
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillReceipt
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitRow
import com.quare.blitzsplit.shared.storescreenshots.SampleBill
import com.quare.blitzsplit.shared.storescreenshots.SampleTrip
import java.text.DecimalFormatSymbols
import java.util.Locale

private const val GROUP_ID = "trip"

/** "Itens da conta" of the trattoria dinner, everyone marked on what they had. */
@Composable
internal fun ItemsScreenshotContent(trip: SampleTrip) {
    val state = trip.splitBillState(trip.dinner, receipt = null)
    ItemsEditorContent(
        state = state,
        items = checkNotNull(state.itemsEditor),
        isDialog = false,
        snackbarHostState = remember { SnackbarHostState() },
        onEvent = {},
    )
}

/** The new bill right after its receipt was read: amount, description and the six items filled in from the photo. */
@Composable
internal fun ReceiptScreenshotContent(trip: SampleTrip) {
    val state = trip.splitBillState(
        trip.dinner,
        receipt = BillReceipt(
            status = ReceiptStatus.READ,
            itemCount = trip.dinner.items
                ?.items
                ?.size ?: 0,
            hasPhoto = true,
            error = null,
        ),
    )
    SplitBillContent(
        state = state.copy(itemsEditor = null),
        isDialog = false,
        snackbarHostState = remember { SnackbarHostState() },
        onWhenClick = {},
        onTimeClick = {},
        onEvent = {},
    )
}

internal val SampleTrip.dinner: SampleBill
    get() = bills.first { it.items != null }

internal fun SampleTrip.billPeople(): List<BillPerson> = people.indices.map { person ->
    BillPerson(
        id = memberIdOf(person),
        name = people[person],
        initials = initialsOf(person),
        avatarColor = avatarColorOf(person),
        photoUrl = photoUrlOf(person),
        isMe = person == me,
        hasJoined = true,
    )
}

/** An amount as the field shows it in [Locale.getDefault], which the generator sets to the shot's language. */
internal fun amountTextOf(
    amountMinor: Long,
    bill: SampleBill,
): String = AmountInput.format(
    amountMinor = amountMinor,
    currency = bill.currency,
    showsSats = false,
    decimalSeparator = DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator,
)

private fun SampleTrip.splitBillState(
    bill: SampleBill,
    receipt: BillReceipt?,
): SplitBillUiState {
    val people = billPeople()
    val payerIds = bill.payers.map { memberIdOf(it.person) }
    val shares = sharesOf(bill)
    val items = bill.items
    val split = SplitEditor(
        mode = if (items == null) SplitMode.EQUAL else SplitMode.ITEMS,
        rows = people.mapIndexed { index, person ->
            SplitRow(
                person = person,
                isIncluded = true,
                valueText = "",
                parts = 1L,
                shareMinor = shares[index],
            )
        },
        totalMinor = bill.totalMinor,
        payerIds = payerIds,
        currency = bill.currency,
        showsSats = false,
        items = items,
    )
    val spentAt = momentOf(bill.daysAgo, bill.minuteOfDay)
    return SplitBillUiState(
        isEditing = false,
        groupId = GROUP_ID,
        pickedGroupId = GROUP_ID,
        groupName = groupName,
        groupInitials = previewInitialsOf(groupName),
        groupColor = AvatarColor.PEACH,
        groupPhotoUrl = null,
        groups = listOf(
            BillGroup(
                id = GROUP_ID,
                name = groupName,
                initials = previewInitialsOf(groupName),
                color = AvatarColor.PEACH,
                photoUrl = null,
                memberCount = people.size,
            ),
        ),
        groupSelection = null,
        currency = bill.currency,
        pickedCurrency = null,
        showsSats = false,
        people = people,
        amountText = amountTextOf(bill.totalMinor, bill),
        description = bill.description,
        hasEditedAmount = true,
        hasEditedDescription = true,
        payerId = payerIds.first(),
        severalPayers = null,
        payerSelection = null,
        payerAmounts = null,
        spentAt = spentAt,
        spentDay = relativeDayOf(bill.daysAgo),
        latestSpentAt = spentAt,
        split = split,
        splitEditor = null,
        isSplitEditorAsDefaultChecked = false,
        itemsEditor = items?.let {
            ItemsEditor(
                split = it,
                people = people,
                totalMinor = bill.totalMinor,
                payerIds = payerIds,
                currency = bill.currency,
                showsSats = false,
            )
        },
        itemEditor = null,
        itemDeletion = null,
        feeEditor = null,
        defaultSplit = null,
        defaultSplitExceptionId = null,
        splitAsDefault = null,
        categories = CategoryTree.appOnly,
        categoryId = bill.categoryKey,
        isCategoryPicked = true,
        categoryMatches = emptyList(),
        categoryUsage = emptyMap(),
        categoryPicker = null,
        categoryForm = null,
        categoryDeletion = null,
        receipt = receipt,
        receiptCategoryId = null,
        receiptRequest = null,
        conflict = null,
        settledPair = null,
        deletion = null,
        isOnline = true,
        isSaving = false,
        isDeleting = false,
        isLoaded = true,
    )
}
