package com.quare.blitzsplit.feature.activity.presentation.model

/*
 * Why: who deleted the item's bill or group and when, which the item says instead of opening it ("Pastel na praia foi
 * apagada por Bruno na sexta"); unknown when the deletion is older than what the device keeps.
 */
data class ActivityDeletion(
    val isGroup: Boolean,
    // Why: the bill's description or the group's name.
    val subject: String?,
    val byName: String?,
    val byMe: Boolean,
    val atMillis: Long?,
)
