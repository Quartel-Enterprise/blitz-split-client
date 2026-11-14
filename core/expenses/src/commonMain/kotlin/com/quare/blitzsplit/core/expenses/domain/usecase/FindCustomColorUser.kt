package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor

fun interface FindCustomColorUser {
    /*
     * Why: a color in use can't be deleted ("Food uses this color"): the first main category the person can see with it,
     * one of the app's on this device or one of any group's on the device; null when none has it.
     */
    suspend operator fun invoke(color: CustomColor): Category?
}
