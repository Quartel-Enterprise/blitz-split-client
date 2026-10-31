package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow

// Why: what the groups' data gives the home, kept between its changes so a minute passing only ages the rows.
internal data class GroupsOnHome(
    val rows: List<GroupRow>,
    val pendingChangeCount: Int,
)
