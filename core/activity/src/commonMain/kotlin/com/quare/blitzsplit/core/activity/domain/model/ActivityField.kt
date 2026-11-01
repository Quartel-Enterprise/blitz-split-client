package com.quare.blitzsplit.core.activity.domain.model

// Why: a field an edit changed, by the server's name in data.changes; each UI names it ("Valor", "Divisão"…).
enum class ActivityField(
    val code: String,
) {
    AMOUNT("amount"),
    DESCRIPTION("description"),
    SPENT_AT("spent_at"),
    PAYERS("payers"),
    SPLIT("split"),
    NAME("name"),
    TYPE("type"),
    COLOR("color"),
    CURRENCY("currency"),
    DEFAULT_SPLIT("default_split"),
    ;

    companion object {
        fun fromCodeOrNull(code: String): ActivityField? = entries.firstOrNull { it.code == code }
    }
}
