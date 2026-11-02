package com.quare.blitzsplit.core.expenses.domain.model

// Why: who opens a bill's share link (7.3, "Quem pode abrir"), by the code the server keeps.
enum class BillLinkVisibility(
    val code: String,
) {
    // Why: "Só quem está na conta", who opens it with their own account.
    MEMBERS("members"),

    // Why: "Qualquer pessoa com o link", who sees it without signing in.
    ANYONE("anyone"),
    ;

    companion object {
        fun fromCodeOrNull(code: String?): BillLinkVisibility? = entries.firstOrNull { it.code == code }
    }
}
