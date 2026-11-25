package com.quare.blitzsplit.core.model

/*
 * Why: the currencies a group can split in, in the order the "Moeda" picker lists them, the cryptocurrencies last
 * under their own header. The code is the ISO 4217 one Room and the API store (BTC and ETH, which ISO doesn't list,
 * by the code exchanges use); each UI names it in its own language. The regions pick the default of a new group, so
 * a group starts in the currency of the device's region, never in a cryptocurrency.
 */
class GroupCurrency private constructor(
    val code: String,
    private val regions: Set<String>,
    /*
     * Why: the digits of the minor unit, as ISO 4217 has them, so an amount is stored as a whole number of it. The
     * rupiah, the New Taiwan dollar, the forint, the Costa Rican colón, the Cuban peso and the Tanzanian shilling are the
     * exceptions: ISO gives each 2, but no price or receipt in Indonesia, Taiwan, Hungary, Costa Rica, Cuba or Tanzania
     * shows cents, so they have none.
     */
    val fractionDigits: Int = 2,
    // Why: "Criptomoedas" in the picker, and shown with only the decimals an amount needs.
    val isCrypto: Boolean = false,
    // Why: false for a code only a newer app or server knows (see `fromCode`), shown by its raw code, never edited.
    val isKnown: Boolean = true,
) {
    val minorPerMajor: Long = (1..fractionDigits).fold(1L) { units, _ -> units * 10 }

    // Why: a currency is its code: the same unknown code read twice is the same currency, never another one's.
    override fun equals(other: Any?): Boolean = other is GroupCurrency && other.code == code

    override fun hashCode(): Int = code.hashCode()

    override fun toString(): String = code

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
        val BRL = GroupCurrency("BRL", setOf("BR"))
        val USD = GroupCurrency("USD", setOf("US", "EC", "SV", "PA", "PR"))
        val EUR = GroupCurrency(
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
        )
        val GBP = GroupCurrency("GBP", setOf("GB"))
        val ARS = GroupCurrency("ARS", setOf("AR"))
        val CLP = GroupCurrency("CLP", setOf("CL"), fractionDigits = 0)
        val COP = GroupCurrency("COP", setOf("CO"))
        val MXN = GroupCurrency("MXN", setOf("MX"))
        val PEN = GroupCurrency("PEN", setOf("PE"))
        val UYU = GroupCurrency("UYU", setOf("UY"))
        val PYG = GroupCurrency("PYG", setOf("PY"), fractionDigits = 0)
        val BOB = GroupCurrency("BOB", setOf("BO"))
        val VES = GroupCurrency("VES", setOf("VE"))
        val CRC = GroupCurrency("CRC", setOf("CR"), fractionDigits = 0)
        val GTQ = GroupCurrency("GTQ", setOf("GT"))
        val HNL = GroupCurrency("HNL", setOf("HN"))
        val NIO = GroupCurrency("NIO", setOf("NI"))
        val DOP = GroupCurrency("DOP", setOf("DO"))
        val CUP = GroupCurrency("CUP", setOf("CU"), fractionDigits = 0)
        val CAD = GroupCurrency("CAD", setOf("CA"))
        val AUD = GroupCurrency("AUD", setOf("AU"))
        val JPY = GroupCurrency("JPY", setOf("JP"), fractionDigits = 0)
        val CHF = GroupCurrency("CHF", setOf("CH", "LI"))
        val PLN = GroupCurrency("PLN", setOf("PL"))
        val TRY = GroupCurrency("TRY", setOf("TR"))
        val IDR = GroupCurrency("IDR", setOf("ID"), fractionDigits = 0)
        val RUB = GroupCurrency("RUB", setOf("RU"))
        val UAH = GroupCurrency("UAH", setOf("UA"))
        val KRW = GroupCurrency("KRW", setOf("KR"), fractionDigits = 0)
        val INR = GroupCurrency("INR", setOf("IN"))
        val SAR = GroupCurrency("SAR", setOf("SA"))
        val AED = GroupCurrency("AED", setOf("AE"))
        val EGP = GroupCurrency("EGP", setOf("EG"))
        val MAD = GroupCurrency("MAD", setOf("MA"))
        val CNY = GroupCurrency("CNY", setOf("CN"))
        val ILS = GroupCurrency("ILS", setOf("IL"))
        val THB = GroupCurrency("THB", setOf("TH"))
        val VND = GroupCurrency("VND", setOf("VN"), fractionDigits = 0)
        val TWD = GroupCurrency("TWD", setOf("TW"), fractionDigits = 0)
        val SEK = GroupCurrency("SEK", setOf("SE"))
        val CZK = GroupCurrency("CZK", setOf("CZ"))
        val RON = GroupCurrency("RON", setOf("RO"))
        val BDT = GroupCurrency("BDT", setOf("BD"))
        val MYR = GroupCurrency("MYR", setOf("MY"))
        val HUF = GroupCurrency("HUF", setOf("HU"), fractionDigits = 0)
        val PKR = GroupCurrency("PKR", setOf("PK"))
        val PHP = GroupCurrency("PHP", setOf("PH"))
        val RSD = GroupCurrency("RSD", setOf("RS"))
        val NOK = GroupCurrency("NOK", setOf("NO", "SJ", "BV"))
        val KES = GroupCurrency("KES", setOf("KE"))
        val TZS = GroupCurrency("TZS", setOf("TZ"), fractionDigits = 0)
        val DKK = GroupCurrency("DKK", setOf("DK", "GL", "FO"))
        val MMK = GroupCurrency("MMK", setOf("MM"), fractionDigits = 0)
        val KHR = GroupCurrency("KHR", setOf("KH"), fractionDigits = 0)

        // Why: no decimals, as the rupiah: ISO gives it 2, but no price in Iran shows a fraction of a rial.
        val IRR = GroupCurrency("IRR", setOf("IR"), fractionDigits = 0)

        // Why: 8 decimals, a satoshi each, which typing or reading "Bitcoin em satoshis" shows as whole sats.
        val BTC = GroupCurrency(
            "BTC",
            emptySet(),
            fractionDigits = 8,
            isCrypto = true,
        )

        // Why: 8 decimals, not ETH's 18 (wei), which a Long of the minor unit holds only up to about 9 ether.
        val ETH = GroupCurrency(
            "ETH",
            emptySet(),
            fractionDigits = 8,
            isCrypto = true,
        )

        // Why: the currencies the app can split in, in the order of the "Moeda" picker.
        val entries: List<GroupCurrency> = listOf(
            BRL,
            USD,
            EUR,
            GBP,
            ARS,
            CLP,
            COP,
            MXN,
            PEN,
            UYU,
            PYG,
            BOB,
            VES,
            CRC,
            GTQ,
            HNL,
            NIO,
            DOP,
            CUP,
            CAD,
            AUD,
            JPY,
            CHF,
            PLN,
            TRY,
            IDR,
            RUB,
            UAH,
            KRW,
            INR,
            SAR,
            AED,
            EGP,
            MAD,
            CNY,
            ILS,
            THB,
            VND,
            TWD,
            SEK,
            CZK,
            RON,
            BDT,
            MYR,
            HUF,
            PKR,
            PHP,
            RSD,
            IRR,
            NOK,
            KES,
            TZS,
            DKK,
            MMK,
            KHR,
            BTC,
            ETH,
        )

        // Why: a region without a currency of the list, or no region at all, splits in dollars.
        val fallback: GroupCurrency = USD

        fun ofRegion(region: String?): GroupCurrency {
            val code = region?.uppercase() ?: return fallback
            return entries.firstOrNull { code in it.regions } ?: fallback
        }

        fun fromCodeOrNull(code: String): GroupCurrency? = entries.firstOrNull { it.code == code }

        /*
         * Why: a bill or payment a newer app wrote in a code this version doesn't list is still shown, by its raw
         * code and its two decimals, and is its own currency in every balance. It is read-only: never offered by
         * `entries`, and nothing new is created in it.
         */
        fun fromCode(code: String): GroupCurrency = fromCodeOrNull(code) ?: GroupCurrency(
            code = code,
            regions = emptySet(),
            isKnown = false,
        )

        private const val DECIMAL_BASE = 10L
    }
}
