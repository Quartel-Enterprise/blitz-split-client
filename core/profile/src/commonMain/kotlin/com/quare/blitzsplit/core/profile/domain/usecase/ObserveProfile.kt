package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

fun interface ObserveProfile {
    operator fun invoke(): Flow<UserProfile?>
}
