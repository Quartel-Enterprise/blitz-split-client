package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.domain.usecase.impl.MarkBillPayingMethodUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveBillPayingMethodUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObservePayingMethodsUseCase
import com.quare.blitzsplit.core.expenses.testing.FakePayingMethodRepository
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.BillPayingMethodUseCases

// "Paid with" over an in-memory repository, empty unless a test hands it methods and marks.
internal fun paidWithUseCasesOf(
    repository: FakePayingMethodRepository = FakePayingMethodRepository(),
): BillPayingMethodUseCases = BillPayingMethodUseCases(
    observePayingMethods = ObservePayingMethodsUseCase(repository),
    observeBillPayingMethod = ObserveBillPayingMethodUseCase(repository),
    markBillPayingMethod = MarkBillPayingMethodUseCase(repository),
)
