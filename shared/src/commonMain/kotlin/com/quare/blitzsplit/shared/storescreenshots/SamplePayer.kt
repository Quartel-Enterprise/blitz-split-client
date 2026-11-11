package com.quare.blitzsplit.shared.storescreenshots

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
data class SamplePayer(
    val person: Int,
    val amountMinor: Long,
)
