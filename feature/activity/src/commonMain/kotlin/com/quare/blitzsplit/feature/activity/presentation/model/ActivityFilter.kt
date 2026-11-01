package com.quare.blitzsplit.feature.activity.presentation.model

import com.quare.blitzsplit.core.activity.domain.model.ActivityType

/*
 * Why: "Filtrar": one group or all (null), one or more types (none is every type), and the person a contact's history
 * opened it on (PeopleBalances.keyOf). The badge counts everything whatever is picked.
 */
data class ActivityFilter(
    val groupId: String?,
    // Why: in ActivityType's order, so the chips and the sheet list them the same way.
    val types: List<ActivityType>,
    val personKey: String?,
) {
    val isActive: Boolean
        get() = groupId != null || types.isNotEmpty() || personKey != null

    companion object {
        val none: ActivityFilter = ActivityFilter(
            groupId = null,
            types = emptyList(),
            personKey = null,
        )
    }
}
