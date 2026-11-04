package com.quare.blitzsplit.feature.billdetail.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SharedBill
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillLink
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.CommentOnSharedBill
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSharedBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.EditSharedBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.LikeSharedBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.LoadSharedBill
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.findComment
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.payersOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.peopleOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.replyTargetOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.sharedTilesOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.threadsOf
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentShare
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillReplyTarget
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillStatus
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.uuid.Uuid

/*
 * Why: a bill's link opened on the web (7.3h). The server answers what it shows, so it is read once online, and again
 * after a comment goes up. Signing in is the app's own (the welcome, or the profile's Google or Apple), and whoever is
 * in the group with the bill on this device may open it in the app instead.
 */
class SharedBillViewModel(
    private val route: Route.SharedBill,
    private val navigator: Navigator,
    private val loadSharedBill: LoadSharedBill,
    private val commentOnSharedBill: CommentOnSharedBill,
    private val editSharedBillComment: EditSharedBillComment,
    private val deleteSharedBillComment: DeleteSharedBillComment,
    private val likeSharedBillComment: LikeSharedBillComment,
    private val observeExpense: ObserveExpense,
    private val observeProfile: ObserveProfile,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    observeAccountStatus: ObserveAccountStatus,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<SharedBillUiState>
        field = MutableStateFlow(
            SharedBillUiState(
                status = SharedBillStatus.LOADING,
                isOnline = true,
                isSignedIn = false,
                hasLinkedAccount = false,
                canOpenInApp = false,
                groupName = "",
                currency = GroupCurrency.BRL,
                description = "",
                totalMinor = 0L,
                spentAt = 0L,
                spentDay = RelativeDay.OTHER,
                categoryId = null,
                categories = CategoryTree.of(emptyList()),
                splitMode = SplitMode.EQUAL,
                payers = emptyList(),
                people = emptyList(),
                attachments = emptyList(),
                viewedAttachmentId = null,
                threads = emptyList(),
                commentDraft = "",
                replyTarget = null,
                isSendingComment = false,
                editingCommentId = null,
                commentToDelete = null,
                likesCommentId = null,
                focusedCommentId = null,
                isChangingComment = false,
            ),
        )

    val uiAction: SharedFlow<SharedBillUiAction>
        field = MutableSharedFlow<SharedBillUiAction>()

    val stateWatcher: FlowWatcher<SharedBillUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<SharedBillUiAction> = FlowWatcher(uiAction)

    private val reloadable = setOf(SharedBillStatus.LOADING, SharedBillStatus.FAILED)
    private var shown: SharedBill.Open? = null
    private var loading: Job? = null
    private var watchingExpense: Job? = null

    // Why: the comment typed keeps its id until it is up, so sending it again after a lost answer posts it once.
    private var commentId: String? = null

    private val edit = CommentComposerEdit()

    // Why: the comment the link was shared at is looked for once, when the bill first shows.
    private var focusCommentId: String? = route.commentId

    // Why: whose session the page was last read with, to read it again once another one signs in.
    private var accountUserId: String? = null
    private var isAccountKnown = false

    init {
        // Why: any session comments, an anonymous one too, as the server lets it; only a device that never connected can't.
        observeAccountStatus()
            .onEach { status ->
                uiState.update {
                    it.copy(
                        isSignedIn = status !is AccountStatus.LocalOnly,
                        hasLinkedAccount = status is AccountStatus.Connected,
                    )
                }
                val userId = userIdOf(status)
                /*
                 * Why: a browser sign in coming back reopens the page while its session is still being made, so a
                 * page read before it (a link only the group opens, or not knowing a member) is read again with it.
                 */
                if (isAccountKnown && userId != accountUserId && uiState.value.isOnline) reload()
                accountUserId = userId
                isAccountKnown = true
            }.launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline ->
                uiState.update { it.copy(isOnline = isOnline) }
                // Why: a page that couldn't load tries again on its own once the internet is back; offline, it says so.
                if (isOnline && uiState.value.status in reloadable) {
                    load()
                } else if (!isOnline && uiState.value.status == SharedBillStatus.LOADING) {
                    uiState.update { it.copy(status = SharedBillStatus.FAILED) }
                }
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: SharedBillUiEvent) {
        when (event) {
            SharedBillUiEvent.OnRetryClicked -> load()
            SharedBillUiEvent.OnSignInClicked -> signIn()
            SharedBillUiEvent.OnOpenInAppClicked -> openInApp()
            SharedBillUiEvent.OnCloseClicked -> leave()
            is SharedBillUiEvent.OnCommentChanged -> changeComment(event.text)
            SharedBillUiEvent.OnSendCommentClicked -> sendComment()
            is SharedBillUiEvent.OnReplyClicked -> replyTo(event.commentId)
            SharedBillUiEvent.OnReplyCancelled -> answer(null)
            is SharedBillUiEvent.OnAttachmentClicked -> view(event.id)
            is SharedBillUiEvent.OnViewerPageChanged -> view(event.id)
            SharedBillUiEvent.OnViewerDismissed -> uiState.update { it.copy(viewedAttachmentId = null) }
            is SharedBillUiEvent.OnCommentEditClicked -> startEditing(event.commentId)
            SharedBillUiEvent.OnCommentEditCancelled -> stopEditing()
            is SharedBillUiEvent.OnCommentDeleteClicked -> askToDeleteComment(event.commentId)
            SharedBillUiEvent.OnCommentDeleteConfirmed -> deleteComment()
            SharedBillUiEvent.OnCommentDeleteDismissed -> uiState.update { it.copy(commentToDelete = null) }
            is SharedBillUiEvent.OnCommentLikeClicked -> toggleLike(event.commentId)
            is SharedBillUiEvent.OnCommentLikesClicked -> showLikes(event.commentId)
            SharedBillUiEvent.OnCommentLikesDismissed -> uiState.update { it.copy(likesCommentId = null) }
            is SharedBillUiEvent.OnCommentShareClicked -> shareComment(event.commentId)
            SharedBillUiEvent.OnFocusedCommentShown -> uiState.update { it.copy(focusedCommentId = null) }
        }
    }

    private fun load() {
        if (loading?.isActive == true) return
        loading = viewModelScope.launch {
            if (shown == null) uiState.update { it.copy(status = SharedBillStatus.LOADING) }
            loadSharedBill(route.code)
                .onSuccess(::show)
                // Why: a page already drawn keeps what it shows when a reload fails.
                .onFailure { if (shown == null) uiState.update { it.copy(status = SharedBillStatus.FAILED) } }
        }
    }

    // Why: a read already under way may have left before what changed (a comment up, a session), so it starts over.
    private fun reload() {
        loading?.cancel()
        loading = null
        load()
    }

    private fun userIdOf(status: AccountStatus): String? = when (status) {
        AccountStatus.LocalOnly -> null
        is AccountStatus.Anonymous -> status.userId
        is AccountStatus.Connected -> status.userId
    }

    private fun show(bill: SharedBill) {
        when (bill) {
            SharedBill.NotFound -> {
                hide()
                uiState.update { it.copy(status = SharedBillStatus.NOT_FOUND) }
            }

            // Why: a link only the group opens says nothing more, but whether signing in may help.
            SharedBill.MembersOnly -> {
                hide()
                uiState.update { it.copy(status = SharedBillStatus.MEMBERS_ONLY) }
            }

            is SharedBill.Open -> {
                shown = bill
                val nowMillis = currentTimeMillis()
                val timeZone = currentTimeZone()
                uiState.update { state ->
                    state.copy(
                        status = SharedBillStatus.OPEN,
                        groupName = bill.groupName,
                        currency = bill.currency,
                        description = bill.description,
                        totalMinor = bill.amountMinor,
                        spentAt = bill.spentAt,
                        spentDay = SpentDay.relativeDayOf(
                            epochMillis = bill.spentAt,
                            nowMillis = nowMillis,
                            timeZone = timeZone,
                        ),
                        categoryId = bill.categoryId,
                        splitMode = bill.splitMode,
                        payers = payersOf(bill),
                        people = peopleOf(bill),
                        attachments = sharedTilesOf(bill.attachments, nowMillis, timeZone),
                        viewedAttachmentId = state.viewedAttachmentId?.takeIf { id ->
                            bill.attachments.any { it.id == id }
                        },
                        threads = threadsOf(bill.comments, emptyList(), nowMillis, timeZone),
                    )
                }
                if (bill.isMember) watchExpense(bill.expenseId) else stopWatchingExpense()
                focusLinkedComment()
                // Why: the comment being edited was deleted meanwhile, so the edit has nothing to save.
                val editingId = uiState.value.editingCommentId
                if (editingId != null && uiState.value.threads
                        .findComment(editingId)
                        ?.canChange != true
                ) {
                    stopEditing()
                }
            }
        }
    }

    // Why: a link turned off or closed to the group while open shows nothing of the bill, nor "Abrir no app".
    private fun hide() {
        shown = null
        stopWatchingExpense()
    }

    // Why: someone no longer in the group (they left meanwhile) opens it here only, whatever the device still holds.
    private fun stopWatchingExpense() {
        watchingExpense?.cancel()
        watchingExpense = null
        uiState.update { it.copy(canOpenInApp = false) }
    }

    // Why: "Abrir no app" only while the bill is on this device, which a first sync may still be bringing.
    private fun watchExpense(expenseId: String) {
        if (watchingExpense != null) return
        watchingExpense = observeExpense(expenseId)
            .onEach { expense -> uiState.update { it.copy(canOpenInApp = expense != null) } }
            .launchIn(viewModelScope)
    }

    private fun signIn() {
        if (!isOnTop()) return
        viewModelScope.launch {
            if (observeProfile().first() == null) {
                navigator.replaceAll(Route.Welcome)
            } else {
                navigator.navigate(Route.Profile)
            }
        }
    }

    private fun openInApp() {
        val bill = shown ?: return
        if (!uiState.value.canOpenInApp || !isOnTop()) return
        // Why: in one step, so the web never draws Despesas without its Conta in between.
        navigator.replaceAbove(
            size = navigator.backStack.value.size - 1,
            routes = listOf(
                Route.GroupDetail(
                    groupId = bill.groupId,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
                Route.BillDetail(
                    groupId = bill.groupId,
                    expenseId = bill.expenseId,
                    focusCommentId = null,
                ),
            ),
        )
    }

    private fun replyTo(commentId: String) {
        answer(replyTargetOf(uiState.value.threads, commentId) ?: return)
    }

    private fun answer(target: BillReplyTarget?) {
        if (target != uiState.value.replyTarget) commentId = null
        uiState.update { it.copy(replyTarget = target) }
    }

    private fun changeComment(text: String) {
        if (text != uiState.value.commentDraft) commentId = null
        uiState.update { it.copy(commentDraft = text) }
    }

    // Why: straight to the server, so what was typed stays in the field until it is up.
    private fun sendComment() {
        val state = uiState.value
        if (!state.canSendComment) return
        state.editingCommentId?.let { return saveEdit(it) }
        val replyTarget = state.replyTarget
        val id = commentId ?: Uuid.random().toString().also { commentId = it }
        uiState.update { it.copy(isSendingComment = true) }
        viewModelScope.launch {
            val authorName = observeProfile().first()?.displayName
            val result = commentOnSharedBill(
                code = route.code,
                id = id,
                body = state.commentDraft,
                threadId = replyTarget?.threadId,
                replyToName = replyTarget?.answeredName,
                authorName = authorName,
            )
            uiState.update { it.copy(isSendingComment = false) }
            when (result.getOrNull()) {
                // Why: only what was sent goes, so a next comment typed meanwhile stays.
                SharedBillComment.POSTED -> {
                    if (uiState.value.commentDraft == state.commentDraft) commentId = null
                    uiState.update {
                        it.copy(
                            commentDraft = it.commentDraft
                                .takeUnless { draft ->
                                    draft == state.commentDraft
                                }.orEmpty(),
                            replyTarget = it.replyTarget.takeUnless { target -> target == replyTarget },
                        )
                    }
                    reload()
                }

                // Why: the link changed meanwhile, so the page says what it shows now.
                SharedBillComment.NOT_FOUND, SharedBillComment.MEMBERS_ONLY -> reload()

                SharedBillComment.NEEDS_NAME -> uiAction.emit(SharedBillUiAction.ShowNeedsName)

                null -> uiAction.emit(SharedBillUiAction.ShowCommentError)
            }
        }
    }

    // Why: once, on the first bill shown: highlighted when it is there, else "Esse comentário foi apagado".
    private fun focusLinkedComment() {
        val commentId = focusCommentId ?: return
        focusCommentId = null
        if (findCommentOrNull(commentId)?.canInteract == true) {
            uiState.update { it.copy(focusedCommentId = commentId) }
        } else {
            viewModelScope.launch { uiAction.emit(SharedBillUiAction.ShowCommentGone) }
        }
    }

    private fun findCommentOrNull(commentId: String): BillCommentRow? = uiState.value.threads.findComment(commentId)

    private fun startEditing(commentId: String) {
        val comment = findCommentOrNull(commentId)?.takeIf { it.canChange } ?: return
        if (!uiState.value.canChangeComments) return
        val state = uiState.value
        show(
            edit.start(
                comment.id,
                comment.body,
                CommentComposer(state.editingCommentId, state.commentDraft, state.replyTarget),
            ),
        )
    }

    private fun stopEditing() {
        if (uiState.value.isEditingComment) show(edit.stop())
    }

    private fun show(composer: CommentComposer) {
        uiState.update {
            it.copy(
                editingCommentId = composer.editingCommentId,
                commentDraft = composer.draft,
                replyTarget = composer.replyTarget,
            )
        }
    }

    // Why: straight to the server, so the edited text stays in the composer until it is up.
    private fun saveEdit(commentId: String) {
        val body = uiState.value.commentDraft
        change(onPosted = { if (uiState.value.editingCommentId == commentId) stopEditing() }) {
            editSharedBillComment(route.code, commentId, body)
        }
    }

    private fun askToDeleteComment(commentId: String) {
        if (findCommentOrNull(commentId)?.canChange != true || !uiState.value.canChangeComments) return
        uiState.update { it.copy(commentToDelete = commentId) }
    }

    private fun deleteComment() {
        val commentId = uiState.value.commentToDelete ?: return
        uiState.update { it.copy(commentToDelete = null) }
        if (uiState.value.editingCommentId == commentId) stopEditing()
        change { deleteSharedBillComment(route.code, commentId) }
    }

    private fun toggleLike(commentId: String) {
        val comment = findCommentOrNull(commentId)?.takeIf { it.canInteract } ?: return
        change { likeSharedBillComment(route.code, comment.id, !comment.isLikedByMe) }
    }

    /*
     * Why: one change at a time, read again from the server once it answered; a link changed meanwhile reads again too,
     * so the page says what it shows now.
     */
    private fun change(
        onPosted: () -> Unit = {},
        request: suspend () -> Result<SharedBillComment>,
    ) {
        if (!uiState.value.canChangeComments) return
        uiState.update { it.copy(isChangingComment = true) }
        viewModelScope.launch {
            val outcome = request().getOrNull()
            uiState.update { it.copy(isChangingComment = false) }
            when (outcome) {
                null -> uiAction.emit(SharedBillUiAction.ShowCommentChangeError)

                SharedBillComment.POSTED -> {
                    onPosted()
                    reload()
                }

                else -> reload()
            }
        }
    }

    private fun showLikes(commentId: String) {
        if ((findCommentOrNull(commentId)?.likeCount ?: 0) == 0) return
        uiState.update { it.copy(likesCommentId = commentId) }
    }

    private fun shareComment(commentId: String) {
        val comment = findCommentOrNull(commentId)?.takeIf { it.canInteract } ?: return
        val share = BillCommentShare(
            link = SharedBillLink.of(route.code, comment.id),
            authorName = comment.authorName,
            isMe = comment.isMe,
            billDescription = uiState.value.description,
            body = comment.body,
        )
        viewModelScope.launch { uiAction.emit(SharedBillUiAction.ShareComment(share)) }
    }

    private fun view(id: String) {
        if (uiState.value.attachments.any { it.id == id }) uiState.update { it.copy(viewedAttachmentId = id) }
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }
}
