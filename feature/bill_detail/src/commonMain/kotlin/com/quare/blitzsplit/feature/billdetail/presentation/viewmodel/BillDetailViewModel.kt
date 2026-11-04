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
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLink
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.AddBillAttachment
import com.quare.blitzsplit.core.expenses.domain.usecase.AddBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.LikeBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.EditBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.LoadBillAttachmentImage
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
import com.quare.blitzsplit.core.navigation.openPersonProfile
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.findComment
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.historyOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.myPartNoteOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.payersOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.replyTargetOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.rowsOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.threadsOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.tilesOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.undoOf
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentShare
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentThread
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
import software.quare.loadable.Loadable
import software.quare.loadable.isLoading
import software.quare.loadable.toLoaded
import software.quare.loadable.valueOrNull

class BillDetailViewModel(
    private val route: Route.BillDetail,
    private val navigator: Navigator,
    private val deleteExpense: DeleteExpense,
    private val noticeBillGone: NoticeBillGone,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    private val addBillComment: AddBillComment,
    private val editBillComment: EditBillComment,
    private val deleteBillComment: DeleteBillComment,
    private val likeBillComment: LikeBillComment,
    private val updateExpense: UpdateExpense,
    private val addBillAttachment: AddBillAttachment,
    private val removeBillAttachment: RemoveBillAttachment,
    private val loadBillAttachmentImage: LoadBillAttachmentImage,
    loadBillAttachmentUrls: LoadBillAttachmentUrls,
    loadBillHistory: LoadBillHistory,
    observeFirstDownload: ObserveFirstDownload,
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
                threads = Loadable.Loading,
                commentDraft = "",
                replyTarget = null,
                editingCommentId = null,
                commentToDelete = null,
                likesCommentId = null,
                focusedCommentId = null,
                history = Loadable.Loading,
                isHistoryFromDeviceOnly = false,
                isOnline = true,
                isUndoing = false,
                attachments = Loadable.Loading,
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

    // Why: a restored account's comments and photos only come with its first full sync, so online they load until then.
    private var isCopyWhole = false

    // Why: what was typed before "Editar" put a comment's text in the composer, given back once the edit ends.
    private var draftBeforeEdit = ""
    private var replyBeforeEdit: BillReplyTarget? = null

    // Why: the comment a link opened at is looked for once, when the comments first show.
    private var focusCommentId: String? = route.focusCommentId

    private val historyRead = BillHistoryRead(
        loadBillHistory = loadBillHistory,
        billId = route.expenseId,
        scope = viewModelScope,
        onChanged = ::showHistory,
    )

    /*
     * Why: the JPEG of each photo added here, read once, and kept once it is up: the screen draws it until it closes
     * or the photo goes, with no blink nor second download while its signed URL comes.
     */
    private var localImages: Map<String, ByteArray> = emptyMap()

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
        observeFirstDownload()
            // Why: offline nothing more is coming, so the device's comments and photos show and take new ones.
            .map { download -> download == null || download.isWaitingForInternet }
            .distinctUntilChanged()
            .onEach { isWhole ->
                isCopyWhole = isWhole
                showThreads()
                showAttachments()
            }.launchIn(viewModelScope)
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
                localImages = localImages.filterKeys { id -> attachments.any { it.id == id } } +
                    loadLocalImages(attachments)
                showAttachments()
            }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
        /*
         * Why: the photos already up load through signed URLs, so they wait for the internet; the ones added here
         * don't. Asked again each minute, which the repository answers from its cache until a URL is about to expire,
         * so a bill left open keeps loading them, and one that failed is tried again.
         */
        viewModelScope.launch {
            combine(attachments, connectivityObserver.observe(), minuteTicks()) { attachments, isOnline, _ ->
                attachments.filter { !it.hasPendingImage && it.id !in localImages }.takeIf { isOnline }
            }.collectLatest { remoteAttachments ->
                if (remoteAttachments.isNullOrEmpty()) return@collectLatest
                loadBillAttachmentUrls(remoteAttachments).onSuccess { urls ->
                    imageUrls = imageUrls + urls
                    showAttachments()
                }
            }
        }
        // Why: the server's history loads on its own, so going offline meanwhile shows the notice at once.
        connectivityObserver
            .observe()
            .distinctUntilChanged()
            .onEach { isOnline ->
                uiState.update { it.copy(isOnline = isOnline) }
                historyRead.onConnectivityChanged(isOnline)
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

            is BillDetailUiEvent.OnPersonClicked -> openPerson(event.memberId)

            BillDetailUiEvent.OnEditClicked -> edit()

            BillDetailUiEvent.OnShareClicked -> share()

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

            is BillDetailUiEvent.OnCommentEditClicked -> startEditing(event.commentId)

            BillDetailUiEvent.OnCommentEditCancelled -> stopEditing()

            is BillDetailUiEvent.OnCommentDeleteClicked -> askToDeleteComment(event.commentId)

            BillDetailUiEvent.OnCommentDeleteConfirmed -> deleteComment()

            BillDetailUiEvent.OnCommentDeleteDismissed -> uiState.update { it.copy(commentToDelete = null) }

            is BillDetailUiEvent.OnCommentLikeClicked -> toggleLike(event.commentId)

            is BillDetailUiEvent.OnCommentLikesClicked -> showLikes(event.commentId)

            BillDetailUiEvent.OnCommentLikesDismissed -> uiState.update { it.copy(likesCommentId = null) }

            is BillDetailUiEvent.OnCommentShareClicked -> shareComment(event.commentId)

            BillDetailUiEvent.OnFocusedCommentShown -> uiState.update { it.copy(focusedCommentId = null) }

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
        val tiles = tilesOf(
            attachments = attachments,
            imageUrls = imageUrls,
            localImages = localImages,
            group = group,
            nowMillis = currentTimeMillis(),
            timeZone = currentTimeZone(),
        )
        uiState.update { it.copy(attachments = if (isCopyWhole) tiles.toLoaded() else Loadable.Loading) }
    }

    private suspend fun loadLocalImages(attachments: List<BillAttachment>): Map<String, ByteArray> = attachments
        .filter { it.hasPendingImage && it.id !in localImages }
        .mapNotNull { attachment -> loadBillAttachmentImage(attachment.id).getOrNull()?.let { attachment.id to it } }
        .toMap()

    private fun view(attachmentId: String) {
        if (!uiState.value.hasAttachment(attachmentId)) return
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
                        BillAttachmentAddition.TOO_LARGE -> BillPhotoError.TOO_LARGE
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
        val tiles = state.attachments.valueOrNull() ?: return
        val index = tiles.indexOfFirst { it.id == attachmentId }
        val tile = tiles.getOrNull(index) ?: return
        if (!tile.canRemove || !state.areActionsEnabled) return
        val next = tiles.getOrNull(index + 1) ?: tiles.getOrNull(index - 1)
        if (state.viewedAttachmentId == attachmentId) uiState.update { it.copy(viewedAttachmentId = next?.id) }
        viewModelScope.launch {
            removeBillAttachment(attachmentId).onFailure {
                uiAction.emit(BillDetailUiAction.ShowRemovePhotoError)
            }
        }
    }

    private fun showThreads() {
        val group = shownGroup ?: return
        val threads = threadsOf(comments, group, currentTimeMillis(), currentTimeZone())
        uiState.update { it.copy(threads = if (isCopyWhole) threads.toLoaded() else Loadable.Loading) }
        if (isCopyWhole) focusLinkedComment(threads)
        // Why: the comment being edited was deleted on another device meanwhile, so the edit has nothing to save.
        val editingId = uiState.value.editingCommentId
        if (editingId != null && threads.findComment(editingId)?.canChange != true) stopEditing()
    }

    // Why: once, on the first comments shown: highlighted when it is there, else "Esse comentário foi apagado".
    private fun focusLinkedComment(threads: List<BillCommentThread>) {
        val commentId = focusCommentId ?: return
        focusCommentId = null
        if (threads.findComment(commentId)?.canInteract == true) {
            uiState.update { it.copy(focusedCommentId = commentId) }
        } else {
            viewModelScope.launch { uiAction.emit(BillDetailUiAction.ShowCommentGone) }
        }
    }

    private fun findCommentOrNull(commentId: String): BillCommentRow? =
        uiState.value.threads.valueOrNull()?.findComment(commentId)

    private fun startEditing(commentId: String) {
        val comment = findCommentOrNull(commentId)?.takeIf { it.canChange } ?: return
        if (!uiState.value.areActionsEnabled) return
        if (!uiState.value.isEditingComment) {
            draftBeforeEdit = uiState.value.commentDraft
            replyBeforeEdit = uiState.value.replyTarget
        }
        uiState.update {
            it.copy(
                editingCommentId = comment.id,
                commentDraft = comment.body,
                replyTarget = null,
            )
        }
    }

    private fun stopEditing() {
        if (!uiState.value.isEditingComment) return
        uiState.update {
            it.copy(
                editingCommentId = null,
                commentDraft = draftBeforeEdit,
                replyTarget = replyBeforeEdit,
            )
        }
        draftBeforeEdit = ""
        replyBeforeEdit = null
    }

    // Why: on the device first, offline too, as a new comment; the composer goes back to what was typed before.
    private fun saveEdit(commentId: String) {
        val body = uiState.value.commentDraft
        stopEditing()
        viewModelScope.launch {
            editBillComment(commentId, body).onFailure { uiAction.emit(BillDetailUiAction.ShowCommentChangeError) }
        }
    }

    private fun askToDeleteComment(commentId: String) {
        if (findCommentOrNull(commentId)?.canChange != true || !uiState.value.areActionsEnabled) return
        uiState.update { it.copy(commentToDelete = commentId) }
    }

    // Why: a tombstone, offline too; an edit of it under way ends with it.
    private fun deleteComment() {
        val commentId = uiState.value.commentToDelete ?: return
        uiState.update { it.copy(commentToDelete = null) }
        if (uiState.value.editingCommentId == commentId) stopEditing()
        viewModelScope.launch {
            deleteBillComment(commentId).onFailure { uiAction.emit(BillDetailUiAction.ShowCommentChangeError) }
        }
    }

    // Why: by this device's member, offline too; the heart turns at once since the device's copy changes.
    private fun toggleLike(commentId: String) {
        val comment = findCommentOrNull(commentId)?.takeIf { it.canInteract } ?: return
        if (!uiState.value.areActionsEnabled) return
        val me = shownGroup?.members?.firstOrNull { it.isMe }
        viewModelScope.launch {
            likeBillComment(
                commentId = comment.id,
                groupId = route.groupId,
                isLiked = !comment.isLikedByMe,
                memberId = me?.id,
                likerName = me?.name,
            ).onFailure { uiAction.emit(BillDetailUiAction.ShowCommentChangeError) }
        }
    }

    private fun showLikes(commentId: String) {
        if ((findCommentOrNull(commentId)?.likeCount ?: 0) == 0) return
        uiState.update { it.copy(likesCommentId = commentId) }
    }

    // Why: the comment's link, which opens Conta at it for whoever is in the group, offline too.
    private fun shareComment(commentId: String) {
        val comment = findCommentOrNull(commentId)?.takeIf { it.canInteract } ?: return
        val share = BillCommentShare(
            link = BillCommentLink.of(route.expenseId, comment.id),
            authorName = comment.authorName,
            isMe = comment.isMe,
            billDescription = uiState.value.description,
            body = comment.body,
        )
        viewModelScope.launch { uiAction.emit(BillDetailUiAction.ShareComment(share)) }
    }

    private fun showHistory() {
        val group = shownGroup ?: return
        val expense = expense ?: return
        val history = historyOf(events, expense, group, currentTimeMillis(), currentTimeZone())
        uiState.update {
            it.copy(
                history = if (historyRead.isLoading) Loadable.Loading else history.toLoaded(),
                isHistoryFromDeviceOnly = !historyRead.hasServerAnswered,
            )
        }
    }

    // Why: only while Conta is on top, as Editar: a second tap mustn't stack the sheet twice.
    private fun share() {
        if (!uiState.value.areActionsEnabled || !isOnTop()) return
        navigator.navigate(
            Route.ShareBill(
                groupId = route.groupId,
                expenseId = route.expenseId,
            ),
        )
    }

    private fun replyTo(commentId: String) {
        val threads = uiState.value.threads.valueOrNull() ?: return
        val target = replyTargetOf(threads, commentId) ?: return
        uiState.update { it.copy(replyTarget = target) }
    }

    private fun sendComment() {
        val state = uiState.value
        if (!state.canSendComment) return
        state.editingCommentId?.let { return saveEdit(it) }
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
                replyToName = replyTarget?.answeredName,
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
        // Why: the latest line is the server's to say; while its history loads, an older edit could look latest.
        if (state.isUndoing || !state.areActionsEnabled || state.history.isLoading()) return
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

    // Why: only someone still in the group has a profile, and only Conta on top takes the tap.
    private fun openPerson(memberId: String) {
        val member = shownGroup?.members?.firstOrNull { it.id == memberId }
        if (member != null && isOnTop()) navigator.openPersonProfile(PeopleBalances.keyOf(member), member.isMe)
    }

    private fun edit() {
        if (!uiState.value.areActionsEnabled || !isOnTop()) return
        navigator.navigate(
            Route.SplitBill(
                groupId = route.groupId,
                expenseId = route.expenseId,
                picksGroupFirst = false,
                scansReceipt = false,
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
