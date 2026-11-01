package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.activity.domain.model.ActivityFeed
import kotlinx.coroutines.flow.Flow

fun interface ObserveActivityFeed {
    operator fun invoke(): Flow<ActivityFeed>
}
