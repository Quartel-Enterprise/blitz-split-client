package com.quare.blitzsplit.core.expenses.domain.model

// Why: what a payment method's name may be, for the form and the use case that saves it.
object PayingMethodRules {
    const val MAX_NAME_LENGTH = 30

    fun isNameValid(
        type: PayingMethodType,
        name: String,
    ): Boolean {
        val trimmed = name.trim()
        return trimmed.length <= MAX_NAME_LENGTH && (!type.needsName || trimmed.isNotEmpty())
    }
}
