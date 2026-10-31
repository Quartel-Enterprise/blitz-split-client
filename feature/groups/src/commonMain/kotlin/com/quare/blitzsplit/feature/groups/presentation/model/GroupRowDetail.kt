package com.quare.blitzsplit.feature.groups.presentation.model

/*
 * Why: what the row says under the group's name, the first that applies: "na fila · Mercado", "você recebe 120,00 e
 * deve 218,00", "outros ainda acertam", "convide alguém" (only you and no bill), "todos acertaram em 3 out" in
 * Quitados, or else the last activity ("hoje", "3 dias").
 */
enum class GroupRowDetail {
    QUEUED,
    MIXED,
    OTHERS_STILL_SETTLING,
    INVITE_SOMEONE,
    EVERYONE_SETTLED,
    ACTIVITY,
}
