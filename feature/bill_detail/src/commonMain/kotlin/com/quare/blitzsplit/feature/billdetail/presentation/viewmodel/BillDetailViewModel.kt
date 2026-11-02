package com.quare.blitzsplit.feature.billdetail.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.usecase.LoadBillHistory
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveBillHistory
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.AddBillAttachment
import com.quare.blitzsplit.core.expenses.domain.usecase.AddBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.LoadBillAttachmentUrls
import com.quare.blitzsplit.core.expenses.domain.usecase.NoticeBillGone
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveBillAttachments
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveBillComments
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryTree
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettlements
import com.quare.blitzsplit.core.expenses.domain.usecase.RemoveBillAttachment
import com.quare.blitzsplit.core.expenses.domain.usecase.UpdateExpense
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.historyOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.myPartNoteOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.payersOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.rowsOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.threadsOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.tilesOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.undoOf
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPhotoError
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillReplyTarget
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class BillDetailViewModel(
    private val route: Route.BillDetail,
    private val navigator: Navigator,
    private val deleteExpense: DeleteExpense,
    private val noticeBillGone: NoticeBillGone,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    private val addBillComment: AddBillComment,
    private val updateExpense: UpdateExpense,
    private val addBillAttachment: AddBillAttachment,
    private val removeBillAttachment: RemoveBillAttachment,
    loadBillAttachmentUrls: LoadBillAttachmentUrls,
    loadBillHistory: LoadBillHistory,
    observeGroup: ObserveGroup,
    observeExpense: ObserveExpense,
    observeExpenses: ObserveExpenses,
    observeSettlements: ObserveSettlements,
    observeCategoryTree: ObserveCategoryTree,
    observeBillComments: ObserveBillComments,
    observeBillHistory: ObserveBillHistory,
    observeBillAttachments: ObserveBillAttachments,
    connectivityObserver: NetworkConnectivityObserver,
    minuteTicks: MinuteTicks,
) : ViewModel() {
    val uiState: StateFlow<BillDetailUiState>
        field = MutableStateFlow(
            BillDetailUiState(
                groupName = "",
                currency = GroupCurrency.BRL,
                description = "",
                totalMinor = 0L,
                spentAt = 0L,
                spentDay = RelativeDay.OTHER,
                payers = emptyList(),
                categoryId = null,
                categories = CategoryTree.appOnly,
                isPending = false,
                myBalanceMinor = null,
                myPartNote = null,
                splitMode = SplitMode.EQUAL,
                people = emptyList(),
                deletion = null,
                isDeleting = false,
                isLoaded = false,
                threads = emptyList(),
                commentDraft = "",
                replyTarget = null,
                history = emptyList(),
                isOnline = true,
                isUndoing = false,
                attachments = emptyList(),
                viewedAttachmentId = null,
                isAddingPhoto = false,
            ),
        )

    val uiAction: SharedFlow<BillDetailUiAction>
        field = MutableSharedFlow<BillDetailUiAction>()

    val stateWatcher: FlowWatcher<BillDetailUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<BillDetailUiAction> = FlowWatcher(uiAction)

    private var expense: Expense? = null
    private var myMemberId: String? = null
    private var shownGroup: Group? = null
    private var comments: List<BillComment> = emptyList()
    private var events: List<ActivityEvent> = emptyList()
    private var attachments: List<BillAttachment> = emptyList()
    private var imageUrls: Map<String, String> = emptyMap()

    // Why: one read of the bill's group, shared by the bill and by what deleting it unsettles.
    private val group: SharedFlow<Group?> = observeGroup(route.groupId)
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    // Why: the payment deleting the bill would unsettle (7.4b), the one source "Apagar esta conta?" says it from.
    private val deletionSettledPair: StateFlow<SettledPairChange?> = combine(
        group,
        observeExpenses(route.groupId),
        observeSettlements(route.groupId),
        ::findDeletionSettledPairOrNull,
    ).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /*
     * Why: the group or the bill went while "Editar conta" covered this screen, which then leaves as soon as it is on
     * top again. A bill that went after it showed says so on Despesas do grupo, unless this device deleted it.
     */
    private var isGone = false
    private var isBillGoneAfterShowing = false

    // Why: a sync may bring someone else's edit while the bill is open, or delete it, or the whole group.
    init {
        observeCategoryTree(route.groupId)
            .onEach { tree -> uiState.update { it.copy(categories = tree) } }
            .launchIn(viewModelScope)
        combine(group, observeExpense(route.expenseId), ::Pair)
            .onEach { (group, expense) -> handle(group, expense) }
            .launchIn(viewModelScope)
        deletionSettledPair
            .onEach(::showDeletionSettledPair)
            .launchIn(viewModelScope)
        observeBillComments(route.expenseId)
            .onEach { comments ->
                this.comments = comments
                showThreads()
            }.launchIn(viewModelScope)
        observeBillHistory(route.expenseId)
            .onEach { events ->
                this.events = events
                showHistory()
            }.launchIn(viewModelScope)
        val attachments = observeBillAttachments(route.expenseId)
            .onEach { attachments ->
                this.attachments = attachments
                showAttachments()
            }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
        // Why: the photos already up load through signed URLs, so they wait for the internet; the ones added here don't.
        viewModelScope.launch {
            combine(attachments, connectivityObserver.observe(), ::Pair).collectLatest { (attachments, isOnline) ->
                if (isOnline && attachments.any { it.pendingImage == null && it.id !in imageUrls }) {
                    loadBillAttachmentUrls(attachments).onSuccess { urls ->
                        imageUrls = imageUrls + urls
                        showAttachments()
                    }
                }
            }
        }
        // Why: the server's history loads on its own, so going offline meanwhile shows the notice at once.
        connectivityObserver
            .observe()
            .distinctUntilChanged()
            .onEach { isOnline ->
                uiState.update { it.copy(isOnline = isOnline) }
                if (isOnline) viewModelScope.launch { loadBillHistory(route.expenseId) }
            }.launchIn(viewModelScope)
        // Why: "Hoje" becomes "Ontem" at midnight on a bill left open.
        minuteTicks()
            .map { SpentDay.dateOf(currentTimeMillis(), currentTimeZone()) }
            .distinctUntilChanged()
            .onEach {
                showThreads()
                showHistory()
                showAttachments()
            }.launchIn(viewModelScope)
        navigator.backStack
            .onEach { if (isGone) leave() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BillDetailUiEvent) {
        when (event) {
            BillDetailUiEvent.OnBackClicked -> leave()

            BillDetailUiEvent.OnEditClicked -> edit()

            BillDetailUiEvent.OnDeleteClicked -> askToDelete()

            BillDetailUiEvent.OnDeleteConfirmed -> delete()

            // Why: the question stays while the bill is being deleted, whatever closes it.
            BillDetailUiEvent.OnDeleteDismissed -> uiState.update {
                if (it.isDeleting) {
                    it
                } else {
                    it.copy(
                        deletion = null,
                    )
                }
            }

            is BillDetailUiEvent.OnCommentChanged -> uiState.update { it.copy(commentDraft = event.text) }

            BillDetailUiEvent.OnSendCommentClicked -> sendComment()

            is BillDetailUiEvent.OnReplyClicked -> replyTo(event.commentId)

            BillDetailUiEvent.OnReplyCancelled -> uiState.update { it.copy(replyTarget = null) }

            BillDetailUiEvent.OnUndoClicked -> undo()

            is BillDetailUiEvent.OnPhotoPicked -> addPhoto(event.source)

            BillDetailUiEvent.OnPhotoUnreadable -> viewModelScope.launch {
                uiAction.emit(BillDetailUiAction.ShowPhotoError(BillPhotoError.UNREADABLE))
            }

            is BillDetailUiEvent.OnAttachmentClicked -> view(event.id)

            is BillDetailUiEvent.OnViewerPageChanged -> view(event.id)

            BillDetailUiEvent.OnViewerDismissed -> uiState.update { it.copy(viewedAttachmentId = null) }

            is BillDetailUiEvent.OnRemoveAttachmentClicked -> removeAttachment(event.id)
        }
    }

    private fun handle(
        group: Group?,
        expense: Expense?,
    ) {
        when {
            group == null -> markGone(isBillGoneAfterShowing = false)

            // Why: one that never showed (a stale link) leaves without a word.
            expense == null -> markGone(isBillGoneAfterShowing = this.expense != null)

            else -> show(group, expense)
        }
    }

    private fun findDeletionSettledPairOrNull(
        group: Group?,
        expenses: List<Expense>,
        settlements: List<Settlement>,
    ): SettledPairChange? = SettledPairChange.findDeletionOrNull(route.expenseId, group, expenses, settlements)

    private fun showDeletionSettledPair(settledPair: SettledPairChange?) {
        // Why: the delete in flight takes the bill out of Room, which must not reword the question still shown.
        uiState.update { if (it.isDeleting) it else it.copy(deletion = it.deletion?.copy(settledPair = settledPair)) }
    }

    private fun markGone(isBillGoneAfterShowing: Boolean) {
        isGone = true
        this.isBillGoneAfterShowing = isBillGoneAfterShowing
        leave()
    }

    private fun show(
        group: Group,
        expense: Expense,
    ) {
        this.expense = expense
        shownGroup = group
        val me = group.members.firstOrNull { it.isMe }
        myMemberId = me?.id
        val membersById = group.members.associateBy { it.id }
        uiState.update { state ->
            state.copy(
                groupName = group.name,
                currency = group.currency,
                description = expense.description,
                totalMinor = expense.amountMinor,
                spentAt = expense.spentAt,
                spentDay = relativeDayOf(expense.spentAt),
                payers = payersOf(group, expense, membersById),
                categoryId = expense.categoryId,
                isPending = expense.isPending,
                myBalanceMinor = me?.id?.let(expense::balanceOf),
                myPartNote = me?.let { myPartNoteOf(expense, it, membersById) },
                splitMode = expense.splitMode,
                people = rowsOf(group, expense),
                // Why: the dialog says what deleting it changes as the bill is now, after a sync changed it.
                deletion = state.deletion?.let { BillDeletion.of(expense, me?.id, deletionSettledPair.value) },
                isLoaded = true,
            )
        }
        showThreads()
        showHistory()
        showAttachments()
    }

    private fun showAttachments() {
        val group = shownGroup ?: return
        uiState.update {
            it.copy(attachments = tilesOf(attachments, imageUrls, group, currentTimeMillis(), currentTimeZone()))
        }
    }

    private fun view(attachmentId: String) {
        if (uiState.value.attachments.none { it.id == attachmentId }) return
        uiState.update { it.copy(viewedAttachmentId = attachmentId) }
    }

    // Why: on the device first, offline too, by this device's member; the sync takes it up.
    private fun addPhoto(source: ByteArray) {
        if (!uiState.value.canAddPhoto) return
        uiState.update { it.copy(isAddingPhoto = true) }
        viewModelScope.launch {
            val error = addBillAttachment(
                expenseId = route.expenseId,
                groupId = route.groupId,
                kind = BillAttachmentKind.PHOTO,
                source = source,
                addedByMemberId = myMemberId,
            ).fold(
                onSuccess = { addition ->
                    when (addition) {
                        BillAttachmentAddition.ADDED -> null
                        BillAttachmentAddition.NO_ROOM -> BillPhotoError.NO_ROOM
                        BillAttachmentAddition.UNREADABLE -> BillPhotoError.UNREADABLE
                    }
                },
                onFailure = { BillPhotoError.FAILED },
            )
            uiState.update { it.copy(isAddingPhoto = false) }
            error?.let { uiAction.emit(BillDetailUiAction.ShowPhotoError(it)) }
        }
    }

    // Why: the viewer goes on to the next attachment, else the one before, and closes after the last.
    private fun removeAttachment(attachmentId: String) {
        val state = uiState.value
        val index = state.attachments.indexOfFirst { it.id == attachmentId }
        val tile = state.attachments.getOrNull(index) ?: return
        if (!tile.canRemove || !state.areActionsEnabled) return
        val next = state.attachments.getOrNull(index + 1) ?: state.attachments.getOrNull(index - 1)
        if (state.viewedAttachmentId == attachmentId) uiState.update { it.copy(viewedAttachmentId = next?.id) }
        viewModelScope.launch {
            removeBillAttachment(attachmentId).onFailure {
                uiAction.emit(BillDetailUiAction.ShowRemovePhotoError)
            }
        }
    }

    private fun showThreads() {
        val group = shownGroup ?: return
        uiState.update {
            it.copy(threads = threadsOf(comments, group, currentTimeMillis(), currentTimeZone()))
        }
    }

    private fun showHistory() {
        val group = shownGroup ?: return
        val expense = expense ?: return
        uiState.update {
            it.copy(history = historyOf(events, expense, group, currentTimeMillis(), currentTimeZone()))
        }
    }

    // Why: a reply to a reply joins its thread and names whom it answers.
    private fun replyTo(commentId: String) {
        val thread = uiState.value.threads.firstOrNull { thread ->
            thread.comment.id == commentId || thread.replies.any { it.id == commentId }
        } ?: return
        val comment = (listOf(thread.comment) + thread.replies).first { it.id == commentId }
        uiState.update {
            it.copy(
                replyTarget = BillReplyTarget(
                    threadId = thread.comment.id,
                    commentId = comment.id,
                    name = comment.authorName,
                    isMe = comment.isMe,
                ),
            )
        }
    }

    private fun sendComment() {
        val state = uiState.value
        if (!state.canSendComment) return
        val me = shownGroup?.members?.firstOrNull { it.isMe }
        val replyTarget = state.replyTarget
        uiState.update {
            it.copy(
                commentDraft = "",
                replyTarget = null,
            )
        }
        viewModelScope.launch {
            addBillComment(
                expenseId = route.expenseId,
                groupId = route.groupId,
                body = state.commentDraft,
                threadId = replyTarget?.threadId,
                replyToName = replyTarget?.name?.takeIf { replyTarget.commentId != replyTarget.threadId },
                authorMemberId = me?.id,
                authorName = me?.name,
            ).onFailure {
                // Why: back in the field only while nothing new was typed there, which would be lost.
                uiState.update {
                    if (it.commentDraft.isEmpty() && it.replyTarget == null) {
                        it.copy(
                            commentDraft = state.commentDraft,
                            replyTarget = replyTarget,
                        )
                    } else {
                        it
                    }
                }
                uiAction.emit(BillDetailUiAction.ShowCommentError)
            }
        }
    }

    // Why: the latest edit's "before" saved over the bill, offline too, as a new edit everyone sees.
    private fun undo() {
        val state = uiState.value
        val expense = expense ?: return
        val group = shownGroup ?: return
        val event = events.firstOrNull() ?: return
        if (state.isUndoing || !state.areActionsEnabled) return
        val undo = undoOf(event, expense, group) ?: return
        uiState.update { it.copy(isUndoing = true) }
        viewModelScope.launch {
            updateExpense(
                expenseId = route.expenseId,
                groupId = route.groupId,
                description = undo.description,
                amountMinor = undo.amountMinor,
                payers = undo.payers,
                splitMode = undo.splitMode,
                shares = undo.shares,
                items = null,
                categoryId = undo.categoryId,
                spentAt = undo.spentAt,
            ).onFailure { uiAction.emit(BillDetailUiAction.ShowUndoError) }
            uiState.update { it.copy(isUndoing = false) }
        }
    }

    private fun edit() {
        if (!uiState.value.areActionsEnabled || !isOnTop()) return
        navigator.navigate(
            Route.SplitBill(
                groupId = route.groupId,
                expenseId = route.expenseId,
                picksGroupFirst = false,
            ),
        )
    }

    private fun askToDelete() {
        val expense = expense ?: return
        if (!uiState.value.areActionsEnabled) return
        uiState.update { it.copy(deletion = BillDeletion.of(expense, myMemberId, deletionSettledPair.value)) }
    }

    private fun delete() {
        val state = uiState.value
        if (state.deletion == null || state.isDeleting) return
        uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            deleteExpense(route.expenseId)
                .onSuccess { leave() }
                .onFailure {
                    uiState.update {
                        it.copy(
                            deletion = null,
                            isDeleting = false,
                        )
                    }
                    uiAction.emit(BillDetailUiAction.ShowDeleteError)
                }
        }
    }

    private fun relativeDayOf(epochMillis: Long): RelativeDay = SpentDay.relativeDayOf(
        epochMillis = epochMillis,
        nowMillis = currentTimeMillis(),
        timeZone = currentTimeZone(),
    )

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    private fun leave() {
        if (!isOnTop()) return
        if (isBillGoneAfterShowing) noticeBillGone(route.groupId, route.expenseId)
        navigator.navigateBack()
    }
}
