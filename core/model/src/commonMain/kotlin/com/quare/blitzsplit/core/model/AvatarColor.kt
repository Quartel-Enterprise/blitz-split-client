package com.quare.blitzsplit.core.model

/*
 * Why: the five fixed avatar colors of the design. The code is what Room and the API store, so it
 * never changes even if an entry is renamed.
 */
enum class AvatarColor(
    val code: String,
) {
    BLUE("blue"),
    PEACH("peach"),
    GREEN("green"),
    LILAC("lilac"),
    SAND("sand"),
    ;

    companion object {
        val default: AvatarColor = BLUE

        fun fromCodeOrNull(code: String): AvatarColor? = entries.firstOrNull { it.code == code }
    }
}
