package com.quare.blitzsplit.core.sync.domain.model

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: what the Pix rule (docs/product/general.md) reads about the person, never where the phone is: Pix is offered
 * when any signal holds, so Brazilians abroad keep it and a foreigner in a Brazilian group can still pay by it.
 */
data class PixSignals(
    val appLanguage: AppLanguage,
    // Why: the device's language tag ("pt-BR"), read only while the app follows the system.
    val deviceLanguage: String?,
    // Why: the device's region ("BR"), one extra signal and never the only path.
    val deviceRegion: String?,
    val hasOwnPixKey: Boolean,
    val groupCurrencies: List<GroupCurrency>,
    val hasMemberWithPixKey: Boolean,
) {
    val showsPix: Boolean
        get() = speaksBrazilianPortuguese ||
            hasOwnPixKey ||
            hasMemberWithPixKey ||
            groupCurrencies.any { it == GroupCurrency.BRL } ||
            deviceRegion.equals(BRAZIL, ignoreCase = true)

    // Why: the picked language when it is Portuguese, the system's when it is on "System" and is Brazil's Portuguese.
    private val speaksBrazilianPortuguese: Boolean
        get() = when (appLanguage) {
            AppLanguage.PORTUGUESE -> true
            AppLanguage.SYSTEM -> deviceLanguage.isBrazilianPortuguese()
            else -> false
        }

    private fun String?.isBrazilianPortuguese(): Boolean {
        if (AppLanguage.languageOf(this) != PORTUGUESE) return false
        // Why: pt-PT (Portugal's) is not Brazil's; a bare "pt" or "pt-BR" is.
        val region = this?.split('-', '_')?.drop(1)?.firstOrNull { it.length == 2 }
        return region == null || region.equals(BRAZIL, ignoreCase = true)
    }

    private companion object {
        const val BRAZIL = "BR"
        const val PORTUGUESE = "pt"
    }
}
