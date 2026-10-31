package com.quare.blitzsplit.core.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/*
 * Why: at once, then at the start of each minute of the device's clock, where every time zone's midnight falls. Each
 * wait is a minute at most, so a clock or time zone change, or a device that slept, shows within a minute.
 */
class SystemMinuteTicks(
    private val currentTimeMillis: CurrentTimeMillis,
) : MinuteTicks {
    override fun invoke(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(MINUTE_MILLIS - currentTimeMillis().mod(MINUTE_MILLIS))
        }
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
