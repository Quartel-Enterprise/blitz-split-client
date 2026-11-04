package com.quare.blitzsplit.feature.billdetail.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SharedBill
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.CommentOnSharedBill
import com.quare.blitzsplit.core.expenses.domain.usecase.LoadSharedBill
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.userIdOrNull
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.payersOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.peopleOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.replyTargetOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.sharedTilesOf
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.threadsOf
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
                val userId = status.userIdOrNull()
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

    private fun view(id: String) {
        if (uiState.value.attachments.any { it.id == id }) uiState.update { it.copy(viewedAttachmentId = id) }
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }
}
