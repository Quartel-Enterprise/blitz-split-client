package com.quare.blitzsplit.feature.splitbill.di

import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.BillCategoryUseCases
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.BillPayingMethodUseCases
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/*
 * Why: the route comes in as a parameter (parametersOf(route)), so each group opens its own ViewModel. Spelled out, as
 * the ViewModel takes more than viewModelOf's 22 parameters.
 */
val splitBillModule = module {
    factoryOf(::BillCategoryUseCases)
    factoryOf(::BillPayingMethodUseCases)
    viewModel { parameters ->
        SplitBillViewModel(
            route = parameters.get(),
            createExpense = get(),
            updateExpense = get(),
            deleteExpense = get(),
            noticeBillGone = get(),
            currentTimeMillis = get(),
            currentTimeZone = get(),
            decimalSeparator = get(),
            offerCategoryTeach = get(),
            saveSplitAsDefault = get(),
            readReceipt = get(),
            attachBillImage = get(),
            takePickedReceipt = get(),
            navigator = get(),
            observeGroup = get(),
            observeGroupIncomes = get(),
            observeExpense = get(),
            observeGroupsByActivity = get(),
            observeExpenses = get(),
            observeSettlements = get(),
            categoryUseCases = get(),
            paidWithUseCases = get(),
            observeBitcoinInSats = get(),
            connectivityObserver = get(),
            trackEvent = get(),
        )
    }
}
