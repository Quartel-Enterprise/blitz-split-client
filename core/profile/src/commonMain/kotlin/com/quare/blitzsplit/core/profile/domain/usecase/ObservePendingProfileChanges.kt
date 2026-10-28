package com.quare.blitzsplit.core.profile.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObservePendingProfileChanges {
    operator fun invoke(): Flow<Boolean>
}
