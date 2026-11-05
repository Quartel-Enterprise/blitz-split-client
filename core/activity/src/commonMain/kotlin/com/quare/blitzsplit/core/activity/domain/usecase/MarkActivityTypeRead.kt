package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.model.NotificationType

/*
 * Why: a row of Notifications whose In app was off left its events born read, worked out from the toggle. Before the
 * toggle turns back on, those events are saved read, so they don't light the bell up all at once.
 */
fun interface MarkActivityTypeRead {
    suspend operator fun invoke(type: NotificationType): Result<Unit>
}
