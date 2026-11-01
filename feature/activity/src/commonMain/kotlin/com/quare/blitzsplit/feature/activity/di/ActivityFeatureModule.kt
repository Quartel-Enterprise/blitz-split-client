package com.quare.blitzsplit.feature.activity.di

import com.quare.blitzsplit.feature.activity.presentation.viewmodel.ActivityViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the route comes in as a parameter (parametersOf(route)), with the person a contact's history opens it on.
val activityFeatureModule = module {
    viewModelOf(::ActivityViewModel)
}
