package com.quare.blitzsplit.core.navigation

/*
 * Why: a screen the app may open something over on its own (an update's offer, the store's review card): a home tab, a
 * group's screen or Atividade, no tab since its bell, where a push notification's cold start lands. Never a form, a
 * sheet or a cover whose typing a new top would lose, nor the group opened over Rachar conta, whose form still holds
 * what was typed under it.
 */
fun Route.isRestingScreen(): Boolean =
    isHomeTab() || (this is Route.GroupDetail && !isOverBill) || this is Route.Activity
