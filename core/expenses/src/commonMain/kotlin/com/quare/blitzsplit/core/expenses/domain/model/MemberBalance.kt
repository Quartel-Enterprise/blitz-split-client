package com.quare.blitzsplit.core.expenses.domain.model

// Why: "Você recebe R$ 332,90 e deve R$ 118,00", over every pair a member is in.
data class MemberBalance(
    val receivesMinor: Long,
    val owesMinor: Long,
) {
    // Why: "+R$ 214,90 a receber" when positive, "−R$ 118,00 você deve" when negative.
    val netMinor: Long
        get() = receivesMinor - owesMinor

    // Why: "R$ 0,00 · você está quitado".
    val isSettled: Boolean
        get() = receivesMinor == 0L && owesMinor == 0L

    // Why: "Acertar" only for someone who owes.
    val canSettle: Boolean
        get() = owesMinor > 0L

    /*
     * Why: "Você recebe R$ 332,90 e deve R$ 118,00" under the net only when it adds something: with one side alone it
     * would repeat the net ("+R$ 313,81 a receber" over "Você recebe R$ 313,81"), so no line.
     */
    fun detailOf(isEveryoneSettled: Boolean): BalanceDetail = when {
        isSettled && isEveryoneSettled -> BalanceDetail.EVERYONE_SETTLED
        isSettled -> BalanceDetail.OTHERS_SETTLING
        receivesMinor > 0L && owesMinor > 0L -> BalanceDetail.BREAKDOWN
        else -> BalanceDetail.NONE
    }
}
