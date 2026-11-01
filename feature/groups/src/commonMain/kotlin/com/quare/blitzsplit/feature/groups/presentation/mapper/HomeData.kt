package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow

// Why: what Início reads of the groups, again only when Room changes; the minute's tick only ages it.
internal data class HomeData(
    val groups: List<Group>,
    val rows: List<GroupRow>,
    val latestBills: List<DatedHomeBill>,
    val pendingChangeCount: Int,
)
