package com.quare.blitzsplit.core.model

enum class AccountProvider(
    val code: String,
) {
    GOOGLE("google"),
    APPLE("apple"),
    ;

    companion object {
        fun fromCodeOrNull(code: String): AccountProvider? = entries.firstOrNull { it.code == code }
    }
}
