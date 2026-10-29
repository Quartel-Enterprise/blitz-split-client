package com.quare.blitzsplit.core.model

/*
 * Why: the currencies a group can split in, in the order the "Moeda" picker lists them. The code is the ISO 4217
 * one Room and the API store; each UI names it in its own language. The regions pick the default of a new group,
 * so a group starts in the currency of the device's region.
 */
enum class GroupCurrency(
    val code: String,
    private val regions: Set<String>,
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
    CLP("CLP", setOf("CL")),
    COP("COP", setOf("CO")),
    MXN("MXN", setOf("MX")),
    PEN("PEN", setOf("PE")),
    UYU("UYU", setOf("UY")),
    PYG("PYG", setOf("PY")),
    CAD("CAD", setOf("CA")),
    AUD("AUD", setOf("AU")),
    JPY("JPY", setOf("JP")),
    CHF("CHF", setOf("CH", "LI")),
    ;

    companion object {
        // Why: a region without a currency of the list, or no region at all, splits in dollars.
        val fallback: GroupCurrency = USD

        fun ofRegion(region: String?): GroupCurrency {
            val code = region?.uppercase() ?: return fallback
            return entries.firstOrNull { code in it.regions } ?: fallback
        }

        fun fromCodeOrNull(code: String): GroupCurrency? = entries.firstOrNull { it.code == code }
    }
}
