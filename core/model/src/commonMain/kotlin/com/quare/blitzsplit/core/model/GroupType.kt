package com.quare.blitzsplit.core.model

/*
 * Why: "Viagem", "Casa", "Evento" and "Outro", in the order every UI lists them. The code is what Room and the API
 * store, so it never changes even if an entry is renamed.
 */
enum class GroupType(
    val code: String,
) {
    TRIP("trip"),
    HOME("home"),
    EVENT("event"),
    OTHER("other"),
    ;

    companion object {
        val default: GroupType = TRIP

        fun fromCodeOrNull(code: String): GroupType? = entries.firstOrNull { it.code == code }
    }
}
