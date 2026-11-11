package com.quare.blitzsplit.shared.storescreenshots

import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

/*
 * Why: the store listing screenshots' story (docs/store-listing-screenshots.md), in one language: the iOS capture
 * seeds it into Room and the Play generators build their screens from it, so both stores show the same trip. The
 * people are indexes into people, the first one being whoever holds the phone.
 */
@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
data class SampleTrip(
    val groupName: String,
    val currency: GroupCurrency,
    val foreignCurrency: GroupCurrency,
    val people: List<String>,
    val bills: List<SampleBill>,
    val payment: SamplePayment,
    val sideGroups: List<SampleSideGroup>,
)
