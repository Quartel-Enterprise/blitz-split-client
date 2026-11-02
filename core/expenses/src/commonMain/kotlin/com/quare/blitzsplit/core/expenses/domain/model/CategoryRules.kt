package com.quare.blitzsplit.core.expenses.domain.model

// Why: "Nome com até 30 letras" (7.8), the server's check too.
object CategoryRules {
    const val MAX_NAME_LENGTH = 30

    fun isNameValid(name: String): Boolean = name.trim().length in 1..MAX_NAME_LENGTH

    fun toCapped(name: String): String = name.take(MAX_NAME_LENGTH)
}
