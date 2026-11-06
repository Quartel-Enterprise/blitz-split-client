package com.quare.blitzsplit.core.model

/*
 * Why: the shape Android's "Ícone do grupo" (6.4d) used to offer: "Biscoito", "Pétala", "Quadrado" and "Círculo".
 * Since the beta's feedback no platform offers or draws it any more: every group is its rounded square. It stays as
 * Room, the API and the activity feed store it, for the groups an older Android build gave another shape and their
 * "Forma" edits. The code is what they store, so it never changes even if an entry is renamed.
 */
enum class GroupShape(
    val code: String,
) {
    COOKIE("cookie"),
    PETAL("petal"),
    SQUARE("square"),
    CIRCLE("circle"),
    ;

    companion object {
        // Why: the rounded square every group is made with and drawn as.
        val fallback: GroupShape = SQUARE

        fun fromCodeOrNull(code: String?): GroupShape? = entries.firstOrNull { it.code == code }
    }
}
