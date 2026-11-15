package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: the line under "Seu saldo neste grupo"'s amount, decided once for every UI, which each puts in its own words:
 * "Ninguém deve nada a ninguém aqui.", "Os outros ainda precisam acertar.", "Você recebe R$ 332,90 e deve R$ 118,00",
 * or no line at all when it would only repeat the amount (beta feedback of Nov 15, 2026).
 */
enum class BalanceDetail {
    EVERYONE_SETTLED,
    OTHERS_SETTLING,
    BREAKDOWN,
    NONE,
}
