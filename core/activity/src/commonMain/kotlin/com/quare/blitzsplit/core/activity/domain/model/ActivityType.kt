package com.quare.blitzsplit.core.activity.domain.model

import com.quare.blitzsplit.core.model.NotificationType

// Why: the five types "Filtrar" picks from: Contas, Pagamentos e acertos, Comentários, Pessoas e grupos, Cobranças.
enum class ActivityType(
    // Why: the row of Notifications its kinds sit under unless a kind says otherwise; the rows follow these types.
    val notificationType: NotificationType,
) {
    BILLS(NotificationType.BILLS),
    PAYMENTS(NotificationType.PAYMENTS),

    // Why: a bill's comments (7.3).
    COMMENTS(NotificationType.COMMENTS),
    PEOPLE_AND_GROUPS(NotificationType.PEOPLE_AND_GROUPS),
    CHARGES(NotificationType.PAYMENT_REQUESTS),
}
