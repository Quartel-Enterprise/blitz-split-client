package com.quare.blitzsplit.feature.groups.di

import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.HomeViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.RecoveryCodeCardDismissal
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val groupsModule = module {
    viewModelOf(::GroupsViewModel)
    viewModelOf(::HomeViewModel)
    // Why: Início's recovery code card stays closed until the next launch, whichever Início is drawn.
    singleOf(::RecoveryCodeCardDismissal)
}
