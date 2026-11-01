package com.quare.blitzsplit.feature.activity.presentation.mapper

import com.quare.blitzsplit.feature.activity.presentation.model.ActivityFilter
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityItem

// Why: an item with the people it is about (PeopleBalances.keyOf), which a contact's history filters by.
internal data class FeedEntry(
    val item: ActivityItem,
    val personKeys: Set<String>,
) {
    fun matches(filter: ActivityFilter): Boolean {
        val type = item.kind.type
        return (filter.groupId == null || item.groupId == filter.groupId) &&
            (filter.types.isEmpty() || type in filter.types) &&
            (filter.personKey == null || filter.personKey in personKeys)
    }
}
