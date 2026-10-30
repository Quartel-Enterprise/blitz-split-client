package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface ConsumeDeletedBillNotice {
    // Why: the group's screen took "Esta conta foi apagada" in, so it shows once.
    operator fun invoke(groupId: String)
}
