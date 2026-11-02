package com.quare.blitzsplit.feature.billdetail.di

import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/*
 * Why: the route comes in as a parameter (parametersOf(route)), so each bill opens its own ViewModel. Spelled out
 * since viewModelOf takes at most 22 dependencies.
 */
val billDetailModule = module {
    viewModel { parameters ->
        BillDetailViewModel(
            route = parameters.get(),
            navigator = get(),
            deleteExpense = get(),
            noticeBillGone = get(),
            currentTimeMillis = get(),
            currentTimeZone = get(),
            addBillComment = get(),
            updateExpense = get(),
            addBillAttachment = get(),
            removeBillAttachment = get(),
            loadBillAttachmentImage = get(),
            loadBillAttachmentUrls = get(),
            loadBillHistory = get(),
            observeGroup = get(),
            observeExpense = get(),
            observeExpenses = get(),
            observeSettlements = get(),
            observeCategoryTree = get(),
            observeBillComments = get(),
            observeBillHistory = get(),
            observeBillAttachments = get(),
            connectivityObserver = get(),
            minuteTicks = get(),
        )
    }
}
