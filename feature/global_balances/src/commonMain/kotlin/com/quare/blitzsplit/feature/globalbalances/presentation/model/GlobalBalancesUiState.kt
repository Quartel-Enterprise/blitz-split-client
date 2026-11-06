package com.quare.blitzsplit.feature.globalbalances.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import software.quare.loadable.Loadable
import software.quare.loadable.isLoaded
import software.quare.loadable.map
import software.quare.loadable.valueOrNull

data class GlobalBalancesUiState(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: AvatarColor,
    // Why: a photo picked offline is a data URI until its upload.
    val photoUrl: String?,
    // Why: the web's sidebar names the account under the profile; null without Google or Apple.
    val accountProvider: AccountProvider?,
    /*
     * Why: everyone with an open balance, the largest net first, whichever the filter. Loading until Room is read and
     * while the account's copy on this device is not whole (its groups come before their bills), so it shimmers rather
     * than reading as "Tudo acertado".
     */
    val people: Loadable<List<PersonRow>>,
    /*
     * Why: "Tudo", "Você deve" and "Te devem", one per currency of the groups, the currency with the most groups
     * first; once loaded never empty, so a device with nothing open still says R$ 0,00. Loading as people is.
     */
    val totals: Loadable<List<PeopleTotal>>,
    // Why: "Sem internet. Seus dados aparecem quando reconectar." under the shimmer while the copy waits offline.
    val isWaitingForInternet: Boolean,
    val filter: BalancesFilter,
    val isOnline: Boolean,
    // Why: "Pagamento de Carla registrado." with "Desfazer", once settling up with her closed over Saldos.
    val recordedPayment: PaymentNotice?,
    /*
     * Why: the person a list and detail (an Android tablet lying down, an iPad lying down, the web's desktop, the Mac's
     * inspector) shows beside the list, as last picked; null until someone is picked.
     */
    val pickedPersonId: String?,
) {
    // Why: the people and the totals load together; until then nothing reads as settled or zero.
    val isLoaded: Boolean
        get() = people.isLoaded() && totals.isLoaded()

    // Why: once per state, since the JS presenter and each SwiftUI body pass read it many times. Empty while loading.
    val sections: List<PeopleSection> by lazy {
        listOfNotNull(
            sectionOf(isYouOwe = true).takeIf { filter != BalancesFilter.OWES_YOU },
            sectionOf(isYouOwe = false).takeIf { filter != BalancesFilter.YOU_OWE },
        ).filter { it.rows.isNotEmpty() }
    }

    /*
     * Why: who the pane beside the list shows: the one picked while the filter still lists them, else the filter's first
     * person, so the pane is never empty while there is someone. A phone draws "Por grupo" in the row instead.
     */
    val selectedPerson: PersonRow? by lazy {
        val rows = sections.flatMap { it.rows }
        rows.firstOrNull { it.id == pickedPersonId } ?: rows.firstOrNull()
    }

    val emptyState: BalancesEmptyState? by lazy {
        when {
            !isLoaded || sections.isNotEmpty() -> null
            loadedPeople.isEmpty() -> BalancesEmptyState.SETTLED
            filter == BalancesFilter.YOU_OWE -> BalancesEmptyState.NOTHING_TO_PAY
            else -> BalancesEmptyState.NOTHING_TO_RECEIVE
        }
    }

    // Why: "1 pessoa" under Você deve and "4 pessoas" under Te devem, each person once over every currency; 0 while loading.
    val toPayPeopleCount: Int by lazy { peopleCountOf(owesYou = false) }

    val toReceivePeopleCount: Int by lazy { peopleCountOf(owesYou = true) }

    // Why: the empty states' "para receber de 4 pessoas", in emptyStateTotal's currency.
    val emptyStatePeopleCount: Int
        get() = when (emptyState) {
            BalancesEmptyState.NOTHING_TO_PAY -> emptyStateTotal?.toReceiveCount ?: 0
            BalancesEmptyState.NOTHING_TO_RECEIVE -> emptyStateTotal?.toPayCount ?: 0
            else -> 0
        }

    // Why: the hero and the "Tudo" amount; Loading shimmers them rather than saying R$ 0,00.
    val primaryTotal: Loadable<PeopleTotal>
        get() = totals.map { it.first() }

    /*
     * Why: the empty states' "R$ 312,40 para receber de 4 pessoas", in the currency that still has something open,
     * which needn't be the one with the most groups; null while no empty state shows.
     */
    val emptyStateTotal: PeopleTotal?
        get() {
            val loaded = totals.valueOrNull() ?: return null
            return when (emptyState) {
                BalancesEmptyState.NOTHING_TO_PAY -> loaded.firstOrNull { it.toReceiveMinor > 0 } ?: loaded.first()
                BalancesEmptyState.NOTHING_TO_RECEIVE -> loaded.firstOrNull { it.toPayMinor > 0 } ?: loaded.first()
                BalancesEmptyState.SETTLED -> loaded.first()
                null -> null
            }
        }

    // Why: "Offline · saldos deste aparelho": everything works on what the device has.
    val showsOfflineNotice: Boolean
        get() = !isOnline

    private val loadedPeople: List<PersonRow>
        get() = people.valueOrNull().orEmpty()

    private fun peopleCountOf(owesYou: Boolean): Int = loadedPeople
        .filter { it.owesYou == owesYou }
        .distinctBy { it.personKey }
        .size

    private fun sectionOf(isYouOwe: Boolean): PeopleSection {
        val rows = loadedPeople.filter { (it.netMinor < 0) == isYouOwe }
        return PeopleSection(
            isYouOwe = isYouOwe,
            rows = rows,
            totals = totals
                .valueOrNull()
                .orEmpty()
                .map { total ->
                    CurrencyAmount(
                        currency = total.currency,
                        amountMinor = if (isYouOwe) total.toPayMinor else total.toReceiveMinor,
                    )
                }.filter { it.amountMinor > 0 },
        )
    }
}
