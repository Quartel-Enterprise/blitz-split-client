package com.quare.blitzsplit.core.model

// Why: the code is offered once, after the first sync of a user without Google or Apple.
enum class RecoveryCodePrompt(
    val code: String,
) {
    NOT_DUE("not_due"),
    DUE("due"),
    SNOOZED("snoozed"),
    SAVED("saved"),
    ;

    companion object {
        fun fromCodeOrNull(code: String): RecoveryCodePrompt? = entries.firstOrNull { it.code == code }
    }
}
