package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.LatestBill

// Why: what the groups' data gives the home, kept between its changes so a minute passing only ages the rows.
internal data class GroupsOnHome(
    val rows: List<GroupRow>,
    val pendingChangeCount: Int,
    val latestBill: LatestBill?,
)
