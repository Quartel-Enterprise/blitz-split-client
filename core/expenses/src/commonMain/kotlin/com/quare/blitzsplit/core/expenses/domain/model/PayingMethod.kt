package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: how the person pays ("Payment methods" in Profile), which they mark on the bills they paid ("Paid with"). Private:
 * only they see it, on their devices and in their own export; the group's other members never do.
 */
data class PayingMethod(
    val id: String,
    val type: PayingMethodType,
    // Why: what the person calls it ("Nubank"), empty for none, so each UI shows the type's name instead.
    val name: String,
    // Why: a card's swatch, null for anything but a card.
    val cardColor: CardColor?,
    val isDefault: Boolean,
    val addedAt: Long,
)

enum class PayingMethodType(
    // Why: the server's `type` column.
    val code: String,
    val isCard: Boolean = false,
    // Why: one wallet of cash is enough; cards, accounts and the rest may repeat.
    val isOnePerPerson: Boolean = false,
    // Why: "Other" means nothing without a name.
    val needsName: Boolean = false,
) {
    CREDIT_CARD("credit_card", isCard = true),
    DEBIT_CARD("debit_card", isCard = true),
    CASH("cash", isOnePerPerson = true),
    PIX("pix"),
    BANK_TRANSFER("bank_transfer"),
    BITCOIN("bitcoin"),
    OTHER("other", needsName = true),
    ;

    companion object {
        // Why: a type a newer app version added is skipped, not crashed on.
        fun ofCode(code: String): PayingMethodType? = entries.find { it.code == code }
    }
}

// Why: brand-neutral swatches for a card's icon; no bank or card brand logo is ever drawn.
enum class CardColor(
    val code: String,
) {
    PURPLE("purple"),
    BLUE("blue"),
    GREEN("green"),
    ORANGE("orange"),
    RED("red"),
    PINK("pink"),
    GOLD("gold"),
    GRAPHITE("graphite"),
    ;

    companion object {
        fun ofCode(code: String): CardColor? = entries.find { it.code == code }
    }
}

object PayingMethodRules {
    const val MAX_NAME_LENGTH = 30

    fun isNameValid(
        type: PayingMethodType,
        name: String,
    ): Boolean {
        val trimmed = name.trim()
        return trimmed.length <= MAX_NAME_LENGTH && (!type.needsName || trimmed.isNotEmpty())
    }
}
