package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.customcolor.CustomColorUser
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor

fun interface FindCustomColorUser {
    /*
     * Why: a color in use can't be deleted ("Food uses this color"): the first main category the person can see with it,
     * one of the app's on this device or one of any group's on the device, else the first of their payment methods whose
     * card has it; null when nothing has it.
     */
    suspend operator fun invoke(color: CustomColor): CustomColorUser?
}
