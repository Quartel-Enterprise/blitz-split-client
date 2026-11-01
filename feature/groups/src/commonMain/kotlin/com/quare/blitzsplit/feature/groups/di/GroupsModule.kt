package com.quare.blitzsplit.feature.groups.di

import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// Why: viewModelOf stops at 22 parameters, one fewer than GroupsViewModel takes.
val groupsModule = module {
    viewModel {
        GroupsViewModel(
            retrySync = get(),
            syncNow = get(),
            navigator = get(),
            consumeGroupDeletion = get(),
            updateGroupsLayout = get(),
            leaveGroup = get(),
            deleteGroup = get(),
            observeRecordedPayment = get(),
            consumeRecordedPayment = get(),
            deleteSettlement = get(),
            currentTimeMillis = get(),
            currentTimeZone = get(),
            minuteTicks = get(),
            observeProfile = get(),
            observeAccount = get(),
            observeSyncState = get(),
            observeGroups = get(),
            observeGroupsMoney = get(),
            connectivityObserver = get(),
            observeGroupDeletions = get(),
            observeGroupsLayout = get(),
            observePeopleBalances = get(),
            observeActivityUnreadCount = get(),
        )
    }
}
