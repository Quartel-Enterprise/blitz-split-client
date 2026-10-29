package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.Group
import kotlinx.coroutines.flow.Flow

fun interface ObserveGroup {
    // Why: null once the group is gone from the device, after a sign out or an account switch.
    operator fun invoke(groupId: String): Flow<Group?>
}
