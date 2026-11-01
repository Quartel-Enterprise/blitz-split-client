package com.quare.blitzsplit.feature.groupstats.presentation.model

// Why: who paid the biggest bill, this device's user first; a null name is someone who left the group.
data class StatsPayer(
    val name: String?,
    val isMe: Boolean,
)
