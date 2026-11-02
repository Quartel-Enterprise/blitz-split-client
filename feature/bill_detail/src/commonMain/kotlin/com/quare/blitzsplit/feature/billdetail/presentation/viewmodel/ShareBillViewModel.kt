package com.quare.blitzsplit.feature.billdetail.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.usecase.DisableBillShareLink
import com.quare.blitzsplit.core.expenses.domain.usecase.LoadBillShareLink
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.SetBillShareLink
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.CopiedFeedbackTimer
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillError
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

/*
 * Why: "Compartilhar conta" (7.3) over Conta. Its link is the server's, so it is read once the device is online, and
 * made with "Só quem está na conta" when the bill has none, the safer of the two. It closes once the bill or its group
 * leaves the device, or once the link is turned off.
 */
class ShareBillViewModel(
    private val route: Route.ShareBill,
    private val navigator: Navigator,
    private val loadBillShareLink: LoadBillShareLink,
    private val setBillShareLink: SetBillShareLink,
    private val disableBillShareLink: DisableBillShareLink,
    observeGroup: ObserveGroup,
    observeExpense: ObserveExpense,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<ShareBillUiState>
        field = MutableStateFlow(
            ShareBillUiState(
                description = "",
                totalMinor = 0L,
                currency = GroupCurrency.BRL,
                memberNames = emptyList(),
                link = null,
                visibility = BillLinkVisibility.MEMBERS,
                isOnline = true,
                isLoading = false,
                isUpdating = false,
                hasFailed = false,
                isLinkCopied = false,
            ),
        )

    val uiAction: SharedFlow<ShareBillUiAction>
        field = MutableSharedFlow<ShareBillUiAction>()

    val stateWatcher: FlowWatcher<ShareBillUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<ShareBillUiAction> = FlowWatcher(uiAction)

    private val copiedFeedback = CopiedFeedbackTimer(viewModelScope) { isCopied ->
        uiState.update { it.copy(isLinkCopied = isCopied) }
    }

    init {
        combine(observeGroup(route.groupId), observeExpense(route.expenseId), ::Pair)
            .onEach { (group, expense) ->
                if (group == null || expense == null) return@onEach leave()
                uiState.update { state ->
                    state.copy(
                        description = expense.description,
                        totalMinor = expense.amountMinor,
                        currency = group.currency,
                        memberNames = group.members.filter { it.hasJoined && !it.isMe }.map { it.name },
                    )
                }
            }.launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline ->
                uiState.update { it.copy(isOnline = isOnline) }
                if (isOnline) loadOnce()
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: ShareBillUiEvent) {
        when (event) {
            is ShareBillUiEvent.OnVisibilityPicked -> changeVisibility(event.visibility)

            ShareBillUiEvent.OnCopyClicked -> uiState.value.link?.let { link ->
                viewModelScope.launch { uiAction.emit(ShareBillUiAction.CopyLink(link)) }
                copiedFeedback.start()
            }

            ShareBillUiEvent.OnSendClicked -> uiState.value.link?.let { link ->
                viewModelScope.launch {
                    uiAction.emit(ShareBillUiAction.SendLink(link, uiState.value.description))
                }
            }

            ShareBillUiEvent.OnDisableClicked -> disable()

            ShareBillUiEvent.OnRetryClicked -> if (uiState.value.isOnline) {
                uiState.update { it.copy(hasFailed = false) }
                loadOnce()
            }

            ShareBillUiEvent.OnCloseClicked -> leave()
        }
    }

    // Why: once per sheet, the first time it is online; again only after a failure, through "Tentar de novo".
    private fun loadOnce() {
        val state = uiState.value
        if (state.link != null || state.isLoading || state.hasFailed) return
        uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val loaded = loadBillShareLink(route.expenseId)
            // Why: null is a bill the server doesn't have, one still in the queue among them, so it can be tried again.
            val shown = if (loaded.isSuccess && loaded.getOrNull() == null) {
                setBillShareLink(route.expenseId, BillLinkVisibility.MEMBERS).getOrNull()
            } else {
                loaded.getOrNull()
            }
            uiState.update {
                it.copy(
                    isLoading = false,
                    hasFailed = shown == null,
                )
            }
            shown?.let(::show)
        }
    }

    private fun changeVisibility(visibility: BillLinkVisibility) {
        val state = uiState.value
        if (!state.canChangeLink || visibility == state.visibility) return
        val before = state.visibility
        uiState.update {
            it.copy(
                visibility = visibility,
                isUpdating = true,
            )
        }
        viewModelScope.launch {
            val link = setBillShareLink(route.expenseId, visibility).getOrNull()
            uiState.update { it.copy(isUpdating = false) }
            if (link != null) {
                show(link)
            } else {
                uiState.update { it.copy(visibility = before) }
                uiAction.emit(ShareBillUiAction.ShowError(ShareBillError.UPDATE_FAILED))
            }
        }
    }

    private fun disable() {
        if (!uiState.value.canChangeLink) return
        uiState.update { it.copy(isUpdating = true) }
        viewModelScope.launch {
            disableBillShareLink(route.expenseId)
                .onSuccess { leave() }
                .onFailure {
                    uiState.update { it.copy(isUpdating = false) }
                    uiAction.emit(ShareBillUiAction.ShowError(ShareBillError.DISABLE_FAILED))
                }
        }
    }

    private fun show(link: BillShareLink) {
        uiState.update {
            it.copy(
                link = link.url,
                visibility = link.visibility,
            )
        }
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: only while it is still on top: a native back (a swipe, a sheet dismissed) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }
}
