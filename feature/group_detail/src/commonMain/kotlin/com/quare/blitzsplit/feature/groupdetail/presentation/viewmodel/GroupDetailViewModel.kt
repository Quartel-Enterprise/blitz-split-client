package com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openGroupTab
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupFace
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    private val route: Route.GroupDetail,
    private val navigator: Navigator,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    observeGroup: ObserveGroup,
    observeExpenses: ObserveExpenses,
) : ViewModel() {
    val uiState: StateFlow<GroupDetailUiState>
        field = MutableStateFlow(
            GroupDetailUiState(
                groupName = "",
                groupInitials = "",
                groupColor = AvatarColor.entries.first(),
                currency = GroupCurrency.BRL,
                faces = emptyList(),
                memberCount = 0,
                notJoinedCount = 0,
                totalSpentMinor = 0L,
                days = emptyList(),
                isLoaded = false,
            ),
        )

    val uiAction: SharedFlow<GroupDetailUiAction>
        field = MutableSharedFlow<GroupDetailUiAction>()

    val stateWatcher: FlowWatcher<GroupDetailUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<GroupDetailUiAction> = FlowWatcher(uiAction)

    // Why: the group went while another screen covered this one, which then leaves as soon as it is on top again.
    private var isGroupGone = false

    // Why: a sync may bring other members' bills while the screen is open, or remove the whole group.
    init {
        combine(observeGroup(route.groupId), observeExpenses(route.groupId), ::Pair)
            .onEach { (group, expenses) ->
                isGroupGone = group == null
                if (group == null) leave() else show(group, expenses)
            }.launchIn(viewModelScope)
        navigator.backStack
            .onEach { if (isGroupGone) leave() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: GroupDetailUiEvent) {
        when (event) {
            GroupDetailUiEvent.OnBackClicked -> leave()

            GroupDetailUiEvent.OnSplitBillClicked -> if (isOnTop()) navigator.navigate(Route.SplitBill(route.groupId))

            GroupDetailUiEvent.OnInviteClicked -> if (isOnTop()) navigator.navigate(Route.Invite(route.groupId))

            GroupDetailUiEvent.OnMembersClicked -> navigator.openGroupTab(
                from = route,
                tab = Route.Members(route.groupId, joinedAs = null),
            )

            GroupDetailUiEvent.OnEditGroupClicked -> if (isOnTop()) navigator.navigate(Route.EditGroup(route.groupId))

            GroupDetailUiEvent.OnBalancesTabClicked,
            GroupDetailUiEvent.OnLeaveGroupClicked,
            GroupDetailUiEvent.OnDeleteGroupClicked,
            is GroupDetailUiEvent.OnExpenseClicked,
            -> viewModelScope.launch { uiAction.emit(GroupDetailUiAction.ShowComingSoon) }
        }
    }

    private fun show(
        group: Group,
        expenses: List<Expense>,
    ) {
        val timeZone = currentTimeZone()
        val now = currentTimeMillis()
        val namesById = group.members.associate { it.id to it.name }
        val meId = group.members.firstOrNull { it.isMe }?.id
        val days = expenses
            .groupBy { SpentDay.dateOf(it.spentAt, timeZone) }
            .map { (date, dayExpenses) ->
                ExpenseDay(
                    dayStartMillis = SpentDay.startOf(date, timeZone),
                    relativeDay = SpentDay.relativeDayOf(date, now, timeZone),
                    expenses = dayExpenses.map { toRow(it, namesById, meId) },
                )
            }
        uiState.update {
            it.copy(
                groupName = group.name,
                groupInitials = Initials.of(group.name),
                groupColor = group.color,
                currency = group.currency,
                faces = group.members.take(MAX_FACES).map { member ->
                    GroupFace(
                        initials = Initials.firstOf(member.name),
                        color = member.avatarColor,
                    )
                },
                memberCount = group.members.size,
                notJoinedCount = group.members.count { member -> !member.hasJoined },
                totalSpentMinor = expenses.sumOf { expense -> expense.amountMinor },
                days = days,
                isLoaded = true,
            )
        }
    }

    private fun toRow(
        expense: Expense,
        namesById: Map<String, String>,
        meId: String?,
    ): ExpenseRow {
        val payerId = expense.payers.firstOrNull()?.memberId
        val paid = expense.payers.filter { it.memberId == meId }.sumOf { it.amountMinor }
        val owed = expense.shares.filter { it.memberId == meId }.sumOf { it.amountMinor }
        val isInBill = expense.payers.any { it.memberId == meId } || expense.shares.any { it.memberId == meId }
        return ExpenseRow(
            id = expense.id,
            description = expense.description,
            spentAt = expense.spentAt,
            payerName = payerId?.let(namesById::get),
            payerIsMe = payerId != null && payerId == meId,
            totalMinor = expense.amountMinor,
            myBalanceMinor = if (isInBill) paid - owed else null,
            isPending = expense.isPending,
        )
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }

    private companion object {
        const val MAX_FACES = 4
    }
}
