package com.quare.blitzsplit.core.activity.domain.usecase

import kotlinx.coroutines.flow.Flow

// Why: the badge on Início's bell, which always counts everything unread, whatever the feed's filters.
fun interface ObserveActivityUnreadCount {
    operator fun invoke(): Flow<Int>
}
