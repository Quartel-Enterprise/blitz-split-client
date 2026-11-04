package com.quare.blitzsplit.feature.billdetail.di

import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.ShareBillViewModel
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.SharedBillViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
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
            editBillComment = get(),
            deleteBillComment = get(),
            likeBillComment = get(),
            updateExpense = get(),
            addBillAttachment = get(),
            removeBillAttachment = get(),
            loadBillAttachmentImage = get(),
            loadBillAttachmentUrls = get(),
            loadBillHistory = get(),
            observeFirstDownload = get(),
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
            trackEvent = get(),
        )
    }
    viewModelOf(::ShareBillViewModel)
    viewModelOf(::SharedBillViewModel)
}
