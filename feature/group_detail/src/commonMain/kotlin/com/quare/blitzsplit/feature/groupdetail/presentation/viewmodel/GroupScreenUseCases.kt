package com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel

import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettlements
import com.quare.blitzsplit.core.groups.domain.usecase.ConsumeGroupDeletion
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroupDeletions
import com.quare.blitzsplit.core.groups.domain.usecase.ObservePinnedGroupIds
import com.quare.blitzsplit.core.groups.domain.usecase.SetGroupPinned
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import software.quare.connectivity.NetworkConnectivityObserver

// Why: what Despesas and Saldos both hand their GroupScreen, so each ViewModel takes it whole.
class GroupScreenUseCases(
    internal val navigator: Navigator,
    internal val observeGroup: ObserveGroup,
    internal val observeExpenses: ObserveExpenses,
    internal val observeSettlements: ObserveSettlements,
    internal val observeRecordedPayment: ObserveRecordedPayment,
    internal val consumeRecordedPayment: ConsumeRecordedPayment,
    internal val deleteSettlement: DeleteSettlement,
    internal val observeGroupDeletions: ObserveGroupDeletions,
    internal val consumeGroupDeletion: ConsumeGroupDeletion,
    internal val connectivityObserver: NetworkConnectivityObserver,
    internal val leaveGroup: LeaveGroup,
    internal val deleteGroup: DeleteGroup,
    internal val observePinnedGroupIds: ObservePinnedGroupIds,
    internal val setGroupPinned: SetGroupPinned,
    // Why: Atividade's unread badge on Android's rail beside the group.
    internal val observeActivityUnreadCount: ObserveActivityUnreadCount,
)
