package com.quare.blitzsplit.core.model

/*
 * Why: "Evento", "Viagem", "Casa" and "Outro", in the order every UI lists them. Evento comes first because it is the
 * default: people split more bills at events (a barbecue, a birthday, a dinner) than on trips. The code is what Room
 * and the API store, so it never changes even if an entry is renamed.
 */
enum class GroupType(
    val code: String,
) {
    EVENT("event"),
    TRIP("trip"),
    HOME("home"),
    OTHER("other"),
    ;

    companion object {
        val default: GroupType = EVENT

        fun fromCodeOrNull(code: String): GroupType? = entries.firstOrNull { it.code == code }
    }
}
