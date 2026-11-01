package com.quare.blitzsplit.feature.globalbalances.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.PaymentKey

data class GlobalBalancesUiState(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: AvatarColor,
    // Why: a photo picked offline is a data URI until its upload.
    val photoUrl: String?,
    // Why: the web's sidebar names the account under the profile; null without Google or Apple.
    val accountProvider: AccountProvider?,
    // Why: everyone with an open balance, the largest net first, whichever the filter.
    val people: List<PersonRow>,
    /*
     * Why: "Tudo", "Você deve" and "Te devem", one per currency of the groups, the currency with the most groups
     * first; never empty, so a device with nothing open still says R$ 0,00.
     */
    val totals: List<PeopleTotal>,
    val filter: BalancesFilter,
    // Why: the device user's Pix key that Cobrar offers to include, the default one; null without a Pix key.
    val pixKey: PaymentKey?,
    val charge: ChargeDraft?,
    val isOnline: Boolean,
    // Why: nothing is drawn until Room is read, so the empty state never flashes.
    val isLoaded: Boolean,
    // Why: "Pagamento de Carla registrado." with "Desfazer", once settling up with her closed over Saldos.
    val recordedPayment: PaymentNotice?,
) {
    val sections: List<PeopleSection>
        get() = listOfNotNull(
            sectionOf(isYouOwe = true).takeIf { filter != BalancesFilter.OWES_YOU },
            sectionOf(isYouOwe = false).takeIf { filter != BalancesFilter.YOU_OWE },
        ).filter { it.rows.isNotEmpty() }

    val emptyState: BalancesEmptyState?
        get() = when {
            !isLoaded || sections.isNotEmpty() -> null
            people.isEmpty() -> BalancesEmptyState.SETTLED
            filter == BalancesFilter.YOU_OWE -> BalancesEmptyState.NOTHING_TO_PAY
            else -> BalancesEmptyState.NOTHING_TO_RECEIVE
        }

    // Why: the hero and the "Tudo" amount.
    val primaryTotal: PeopleTotal
        get() = totals.first()

    /*
     * Why: the empty states' "R$ 312,40 para receber de 4 pessoas", in the currency that still has something open,
     * which needn't be the one with the most groups.
     */
    val emptyStateTotal: PeopleTotal
        get() = when (emptyState) {
            BalancesEmptyState.NOTHING_TO_PAY -> totals.firstOrNull { it.toReceiveMinor > 0 }
            BalancesEmptyState.NOTHING_TO_RECEIVE -> totals.firstOrNull { it.toPayMinor > 0 }
            else -> null
        } ?: primaryTotal

    // Why: "Offline · saldos deste aparelho": everything works on what the device has.
    val showsOfflineNotice: Boolean
        get() = !isOnline

    private fun sectionOf(isYouOwe: Boolean): PeopleSection {
        val rows = people.filter { (it.netMinor < 0) == isYouOwe }
        return PeopleSection(
            isYouOwe = isYouOwe,
            rows = rows,
            totals = totals
                .map { total ->
                    CurrencyAmount(
                        currency = total.currency,
                        amountMinor = if (isYouOwe) total.toPayMinor else total.toReceiveMinor,
                    )
                }.filter { it.amountMinor > 0 },
        )
    }
}
