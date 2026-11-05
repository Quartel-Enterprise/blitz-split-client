package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.profile.domain.model.AppInvite
import kotlinx.coroutines.flow.Flow

// Why: null on the platforms that never invite (the phone apps) and while no store lists the app for this one.
fun interface ObserveAppInvite {
    operator fun invoke(): Flow<AppInvite?>
}
