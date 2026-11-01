package com.quare.blitzsplit.feature.groups.di

import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.HomeViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.RecoveryCodeCardDismissal
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val groupsModule = module {
    viewModelOf(::GroupsViewModel)
    // Why: more parameters than viewModelOf takes.
    viewModel {
        HomeViewModel(
            navigator = get(),
            retrySync = get(),
            syncNow = get(),
            updateRecoveryCodePrompt = get(),
            consumeGroupDeletion = get(),
            recoveryCodeCardDismissal = get(),
            observeRecordedPayment = get(),
            observeSettledUp = get(),
            consumeRecordedPayment = get(),
            deleteSettlement = get(),
            currentTimeMillis = get(),
            currentTimeZone = get(),
            minuteTicks = get(),
            observeProfile = get(),
            observeAccount = get(),
            observeSyncState = get(),
            observeFirstDownload = get(),
            observeGroups = get(),
            observeGroupsMoney = get(),
            observePeopleBalances = get(),
            observePinnedGroupIds = get(),
            observeGroupDeletions = get(),
            connectivityObserver = get(),
        )
    }
    // Why: Início's recovery code card stays closed until the next launch, whichever Início is drawn.
    singleOf(::RecoveryCodeCardDismissal)
}
