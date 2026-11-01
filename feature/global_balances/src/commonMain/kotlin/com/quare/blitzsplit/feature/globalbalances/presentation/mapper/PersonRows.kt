package com.quare.blitzsplit.feature.globalbalances.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.PersonBalance
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.feature.globalbalances.presentation.model.PersonRow

internal fun idOf(person: PersonBalance): String = "${person.key}|${person.currency.code}"

/*
 * Why: "Por grupo" opens on a tap, and is already open for someone alone in Você deve or Te devem; a tap on that one
 * closes it, so toggledIds holds whatever differs from the default.
 */
internal fun rowsOf(
    people: List<PersonBalance>,
    filter: BalancesFilter,
    toggledIds: Set<String>,
): List<PersonRow> {
    val inFilter = people.filter { person ->
        when (filter) {
            BalancesFilter.ALL -> true
            BalancesFilter.YOU_OWE -> person.netMinor < 0
            BalancesFilter.OWES_YOU -> person.netMinor > 0
        }
    }
    val isAlone = filter != BalancesFilter.ALL && inFilter.size == 1
    return people.map { person ->
        val id = idOf(person)
        val isOpenByDefault = isAlone && person in inFilter
        PersonRow(
            id = id,
            name = person.name,
            initials = person.name?.let(Initials::of).orEmpty(),
            color = person.face?.color,
            currency = person.currency,
            netMinor = person.netMinor,
            groups = person.groups,
            isGuest = person.isGuest,
            isExpanded = isOpenByDefault != (id in toggledIds),
        )
    }
}
