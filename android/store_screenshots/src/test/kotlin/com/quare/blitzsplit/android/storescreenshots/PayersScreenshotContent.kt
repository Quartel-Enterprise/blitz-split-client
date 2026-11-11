package com.quare.blitzsplit.android.storescreenshots

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.quare.blitzsplit.android.feature.billdetail.BillDetailContent
import com.quare.blitzsplit.android.platform.PhotoPickers
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillAttachmentTile
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentThread
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryDay
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPayer
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillShareRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.MyPartNote
import com.quare.blitzsplit.shared.storescreenshots.SampleBill
import com.quare.blitzsplit.shared.storescreenshots.SampleTrip
import software.quare.loadable.toLoaded

/** The boat tour: two people paid it, in the currency of the place, not the group's. */
@Composable
internal fun PayersScreenshotContent(trip: SampleTrip) {
    BillDetailContent(
        state = trip.billDetailState(trip.boatTour),
        isDialog = false,
        snackbarHostState = remember { SnackbarHostState() },
        pickers = PhotoPickers(
            onTakePhotoClick = null,
            onChoosePhotoClick = {},
        ),
        onEvent = {},
    )
}

internal val SampleTrip.boatTour: SampleBill
    get() = bills.first { it.currency != currency }

private fun SampleTrip.billDetailState(bill: SampleBill): BillDetailUiState {
    val shares = sharesOf(bill)
    val payerIndexes = bill.payers.map { it.person }
    return BillDetailUiState(
        groupName = groupName,
        currency = bill.currency,
        description = bill.description,
        totalMinor = bill.totalMinor,
        spentAt = momentOf(bill.daysAgo, bill.minuteOfDay),
        spentDay = relativeDayOf(bill.daysAgo),
        hasSpentTime = true,
        payers = payerIndexes.map { person ->
            BillPayer(
                memberId = memberIdOf(person),
                name = people[person],
                initials = initialsOf(person),
                avatarColor = avatarColorOf(person),
                photoUrl = photoUrlOf(person),
                isMe = person == me,
                opensProfile = true,
            )
        },
        categoryId = bill.categoryKey,
        categories = CategoryTree.appOnly,
        isPending = false,
        myBalanceMinor = balanceOf(bill, me),
        myPartNote = MyPartNote.OwesPayers(payerIndexes.map { people[it] }),
        splitMode = SplitMode.EQUAL,
        people = people.indices.map { person ->
            BillShareRow(
                memberId = memberIdOf(person),
                name = people[person],
                initials = initialsOf(person),
                avatarColor = avatarColorOf(person),
                photoUrl = photoUrlOf(person),
                isMe = person == me,
                hasJoined = true,
                paidMinor = bill.payers.firstOrNull { it.person == person }?.amountMinor,
                shareMinor = shares[person],
                opensProfile = true,
            )
        },
        deletion = null,
        isDeleting = false,
        isLoaded = true,
        threads = emptyList<BillCommentThread>().toLoaded(),
        commentDraft = "",
        replyTarget = null,
        editingCommentId = null,
        commentToDelete = null,
        likesCommentId = null,
        focusedCommentId = null,
        history = emptyList<BillHistoryDay>().toLoaded(),
        isHistoryFromDeviceOnly = false,
        isOnline = true,
        isUndoing = false,
        attachments = emptyList<BillAttachmentTile>().toLoaded(),
        viewedAttachmentId = null,
        isAddingPhoto = false,
    )
}
