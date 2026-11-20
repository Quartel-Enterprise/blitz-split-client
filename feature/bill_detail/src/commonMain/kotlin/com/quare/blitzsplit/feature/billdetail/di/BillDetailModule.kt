package com.quare.blitzsplit.feature.billdetail.di

import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.ShareBillViewModel
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.SharedBillViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/*
 * Why: the route comes in as a parameter (parametersOf(route)), so each bill opens its own ViewModel. Spelled out
 * since factoryOf takes at most 22 dependencies.
 */
val billDetailModule = module {
    factory { parameters ->
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
            loadCropPhoto = get(),
            prepareBillPhoto = get(),
            attachBillImage = get(),
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
            observeBillPayingMethod = get(),
            observeBillComments = get(),
            observeBillHistory = get(),
            observeBillAttachments = get(),
            connectivityObserver = get(),
            minuteTicks = get(),
            observeSpokenLanguage = get(),
            trackEvent = get(),
        )
    }
    factoryOf(::ShareBillViewModel)
    factoryOf(::SharedBillViewModel)
}
