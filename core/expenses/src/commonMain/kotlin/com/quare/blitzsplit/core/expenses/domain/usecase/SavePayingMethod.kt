package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType

fun interface SavePayingMethod {
    /*
     * Why: adds a method (a null methodId) or edits one. Made default, it takes over from the one that was; a name the
     * type refuses, or a second method of a one-per-person type, fails instead.
     */
    suspend operator fun invoke(
        methodId: String?,
        type: PayingMethodType,
        name: String,
        cardColor: CardColor?,
        isDefault: Boolean,
    ): Result<Unit>
}
