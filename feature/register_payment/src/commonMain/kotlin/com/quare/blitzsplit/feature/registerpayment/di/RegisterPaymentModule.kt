package com.quare.blitzsplit.feature.registerpayment.di

import com.quare.blitzsplit.core.expenses.domain.usecase.RecordSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.SettleUp
import com.quare.blitzsplit.core.support.domain.usecase.impl.toReviewNoting
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.PickPaymentViewModel
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.RegisterPaymentViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/*
 * Why: the route comes in as a parameter (parametersOf(route)), so each pair opens its own ViewModel. Spelled out,
 * since a payment recorded here is a moment the app may ask for a review after, which the two use cases note.
 */
val registerPaymentModule = module {
    viewModel { parameters ->
        RegisterPaymentViewModel(
            route = parameters.get(),
            navigator = get(),
            recordSettlement = get<RecordSettlement>().toReviewNoting(get()),
            settleUp = get<SettleUp>().toReviewNoting(get()),
            consumeRecordedPayment = get(),
            currentTimeMillis = get(),
            currentTimeZone = get(),
            decimalSeparator = get(),
            formatPaymentKey = get(),
            observeMemberPaymentKeys = get(),
            encodeQrCode = get(),
            observeGroup = get(),
            observeGroupBalances = get(),
            observePeopleBalances = get(),
            observeFirstDownload = get(),
            observeBitcoinInSats = get(),
            observeShowsPix = get(),
            connectivityObserver = get(),
            trackEvent = get(),
        )
    }
    viewModelOf(::PickPaymentViewModel)
}
