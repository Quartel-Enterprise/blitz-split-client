package com.quare.blitzsplit.core.expenses.customcolor

import com.quare.blitzsplit.core.expenses.domain.usecase.AddCustomColor
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteCustomColor
import com.quare.blitzsplit.core.expenses.domain.usecase.FindCustomColorUser
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCustomColors

// Why: what CustomColorController needs, injected as one into each screen that has a category color picker.
class CustomColorUseCases(
    val observeCustomColors: ObserveCustomColors,
    val addCustomColor: AddCustomColor,
    val deleteCustomColor: DeleteCustomColor,
    val findCustomColorUser: FindCustomColorUser,
)
