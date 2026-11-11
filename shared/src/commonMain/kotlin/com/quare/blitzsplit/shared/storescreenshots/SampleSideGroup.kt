package com.quare.blitzsplit.shared.storescreenshots

import com.quare.blitzsplit.core.model.GroupType
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

// Why: Home's other pinned groups, each with one bill that someone else paid, so they carry a balance of their own.
@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
data class SampleSideGroup(
    val name: String,
    val type: GroupType,
    val people: List<Int>,
    val billDescription: String,
    val billTotalMinor: Long,
    val billPayer: Int,
    val categoryKey: String,
    val daysAgo: Int,
)
