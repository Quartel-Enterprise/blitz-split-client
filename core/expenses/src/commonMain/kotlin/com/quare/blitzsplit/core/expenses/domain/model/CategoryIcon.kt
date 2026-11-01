package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: the icons a category shows, the app's and those "Nova categoria" offers (7.8), in the board's order. The code is
 * what Room and the server store for a group's main category, so it never changes even if an entry is renamed.
 */
enum class CategoryIcon(
    val code: String,
) {
    FOOD("food"),
    CART("cart"),
    HOME("home"),
    CAR("car"),
    PLANE("plane"),
    TICKET("ticket"),
    REPEAT("repeat"),
    BAG("bag"),
    HEART("heart"),
    BOOK("book"),
    SUN("sun"),
    BALL("ball"),
    PAW("paw"),
    GIFT("gift"),
    GLASS("glass"),
    MUSIC("music"),
    GYM("gym"),
    DOTS("dots"),
    ;

    companion object {
        fun fromCodeOrNull(code: String?): CategoryIcon? = entries.firstOrNull { it.code == code }
    }
}
