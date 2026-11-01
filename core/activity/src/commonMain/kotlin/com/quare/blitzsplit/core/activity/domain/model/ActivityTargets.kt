package com.quare.blitzsplit.core.activity.domain.model

// Why: what is still on the device, so an event whose bill or group is gone keeps its row with the trash icon.
data class ActivityTargets(
    val billIds: Set<String>,
    val groupIds: Set<String>,
)
