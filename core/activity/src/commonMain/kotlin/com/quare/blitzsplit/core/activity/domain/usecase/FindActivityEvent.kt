package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent

fun interface FindActivityEvent {
    // Why: a push notification's tap opens what the event's tap in Atividade opens, the comment included.
    suspend operator fun invoke(eventId: String): ActivityEvent?
}
