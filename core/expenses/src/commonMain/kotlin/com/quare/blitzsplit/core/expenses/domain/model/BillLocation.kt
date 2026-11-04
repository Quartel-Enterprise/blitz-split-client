package com.quare.blitzsplit.core.expenses.domain.model

// Why: where a comment's link leads, for this device's user.
sealed interface BillLocation {
    // Why: the bill is in a group the user is in, on the device or not synced to it yet.
    data class Found(
        val groupId: String,
        val isOnDevice: Boolean,
    ) : BillLocation

    // Why: deleted on this device or on the server, in a group the user is in.
    data class Deleted(
        val groupId: String,
    ) : BillLocation

    // Why: the server knows no such bill the user may see: a group they aren't in, or a link that never was one.
    data object OutOfReach : BillLocation

    // Why: not on the device, and the server couldn't be asked (offline).
    data object Unknown : BillLocation
}
