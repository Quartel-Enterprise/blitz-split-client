package com.quare.blitzsplit.shared.storescreenshots

// Why: the words of the trip in one language, which SampleTrips turns into amounts in that language's currency.
data class SampleTripCopy(
    val groupName: String,
    val people: List<String>,
    val dinner: String,
    val apartment: String,
    val groceries: String,
    val carRental: String,
    val boatTour: String,
    val taxi: String,
    val roommates: String,
    val soccer: String,
    val court: String,
    val items: List<String>,
)
