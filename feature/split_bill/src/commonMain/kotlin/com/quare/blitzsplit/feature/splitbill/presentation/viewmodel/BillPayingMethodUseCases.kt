package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.usecase.MarkBillPayingMethod
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveBillPayingMethod
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePayingMethods

// Why: what "Paid with" reads and writes, one parameter for the ViewModel, which takes more than viewModelOf's 22.
data class BillPayingMethodUseCases(
    val observePayingMethods: ObservePayingMethods,
    val observeBillPayingMethod: ObserveBillPayingMethod,
    val markBillPayingMethod: MarkBillPayingMethod,
)
