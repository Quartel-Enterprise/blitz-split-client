package com.quare.blitzsplit.core.groups.domain.model

// Why: the same bound as the check constraints of public.groups and public.group_members in the API.
object GroupNameRules {
    const val MAX_LENGTH: Int = 40

    fun toCapped(name: String): String = name.takeLetters(MAX_LENGTH)

    fun isWithinLimit(name: String): Boolean = name.letterCount <= MAX_LENGTH

    // Why: the "14/40" under the name, counted as the server counts it.
    fun lengthOf(name: String): Int = name.letterCount
}
