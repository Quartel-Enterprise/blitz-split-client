package com.quare.blitzsplit.core.model

/*
 * Why: the currencies a group can split in, in the order the "Moeda" picker lists them, the cryptocurrencies last
 * under their own header. The code is the ISO 4217 one Room and the API store (BTC and ETH, which ISO doesn't list,
 * by the code exchanges use); each UI names it in its own language. The regions pick the default of a new group, so
 * a group starts in the currency of the device's region, never in a cryptocurrency.
 */
enum class GroupCurrency(
    val code: String,
    private val regions: Set<String>,
    // Why: the digits of the minor unit, as ISO 4217 has them, so an amount is stored as a whole number of it.
    val fractionDigits: Int = 2,
    // Why: "Criptomoedas" in the picker, and shown with only the decimals an amount needs.
    val isCrypto: Boolean = false,
) {
    BRL("BRL", setOf("BR")),
    USD("USD", setOf("US", "EC", "SV", "PA", "PR")),
    EUR(
        "EUR",
        setOf(
            "AT",
            "BE",
            "HR",
            "CY",
            "EE",
            "FI",
            "FR",
            "DE",
            "GR",
            "IE",
            "IT",
            "LV",
            "LT",
            "LU",
            "MT",
            "NL",
            "PT",
            "SK",
            "SI",
            "ES",
            "AD",
            "MC",
            "SM",
            "VA",
            "ME",
            "XK",
        ),
    ),
    GBP("GBP", setOf("GB")),
    ARS("ARS", setOf("AR")),
    CLP("CLP", setOf("CL"), fractionDigits = 0),
    COP("COP", setOf("CO")),
    MXN("MXN", setOf("MX")),
    PEN("PEN", setOf("PE")),
    UYU("UYU", setOf("UY")),
    PYG("PYG", setOf("PY"), fractionDigits = 0),
    CAD("CAD", setOf("CA")),
    AUD("AUD", setOf("AU")),
    JPY("JPY", setOf("JP"), fractionDigits = 0),
    CHF("CHF", setOf("CH", "LI")),

    // Why: 8 decimals, a satoshi each, which typing or reading "Bitcoin em satoshis" shows as whole sats.
    BTC(
        "BTC",
        emptySet(),
        fractionDigits = 8,
        isCrypto = true,
    ),

    // Why: 8 decimals, not ETH's 18 (wei), which a Long of the minor unit holds only up to about 9 ether.
    ETH(
        "ETH",
        emptySet(),
        fractionDigits = 8,
        isCrypto = true,
    ),
    ;

    val minorPerMajor: Long = (1..fractionDigits).fold(1L) { units, _ -> units * 10 }

    /*
     * Why: an amount shown on screen drops its decimals when they are all zero ("R$ 500", never "R$ 500,00"), and
     * keeps every one otherwise ("R$ 12,50"), amount by amount, so a list may mix both. A cryptocurrency keeps only
     * the decimals the amount needs ("₿ 0,00125"), and bitcoin in satoshis none, being whole sats. Fields keep the
     * currency's (`getTypedFractionDigits`).
     */
    fun getDisplayFractionDigits(
        amountMinor: Long,
        showsSats: Boolean,
    ): Int {
        val typedDigits = getTypedFractionDigits(showsSats)
        var fraction = amountMinor % minorPerMajor
        if (typedDigits == 0 || fraction == 0L) return 0
        if (!isCrypto) return fractionDigits
        var digits = fractionDigits
        while (fraction % DECIMAL_BASE == 0L) {
            fraction /= DECIMAL_BASE
            digits--
        }
        return digits
    }

    // Why: the decimals an amount field types and shows: the currency's, none for bitcoin in satoshis (1 sat each).
    fun getTypedFractionDigits(showsSats: Boolean): Int = if (this == BTC && showsSats) 0 else fractionDigits

    companion object {
        // Why: a region without a currency of the list, or no region at all, splits in dollars.
        val fallback: GroupCurrency = USD

        fun ofRegion(region: String?): GroupCurrency {
            val code = region?.uppercase() ?: return fallback
            return entries.firstOrNull { code in it.regions } ?: fallback
        }

        fun fromCodeOrNull(code: String): GroupCurrency? = entries.firstOrNull { it.code == code }

        private const val DECIMAL_BASE = 10L
    }
}
