package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface RestoreCategoryMap {
    // Why: the app's 60 terms come back as they came; the person's own terms stay (9.1f).
    suspend operator fun invoke(): Result<Unit>
}
