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

    // Why: the group icon's shape (6.4d); a group made before shapes has the square.
    SHAPE("shape"),

    // Why: the group's photo, as PHOTO_PREVIOUS before and PHOTO_NEW after, or no code for none: a URL says nothing.
    PHOTO("photo_url"),
    CURRENCY("currency"),
    DEFAULT_SPLIT("default_split"),
    CATEGORY("category"),

    // Why: who may open a bill's share link (7.3e), "members" or "anyone"; null once it was turned off.
    VISIBILITY("visibility"),
    ;

    companion object {
        const val PHOTO_PREVIOUS = "previous"
        const val PHOTO_NEW = "new"

        fun fromCodeOrNull(code: String): ActivityField? = entries.firstOrNull { it.code == code }
    }
}
