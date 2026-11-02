package com.quare.blitzsplit.push

import androidx.annotation.StringRes
import com.quare.blitzsplit.R

/*
 * Why: one channel per toggle of Perfil › Notificações (4.8), named as it is, so the system's settings silence the
 * same groups of notifications the app's toggles do.
 */
enum class PushChannel(
    val id: String,
    @param:StringRes val nameRes: Int,
    @param:StringRes val descriptionRes: Int,
) {
    NEW_EXPENSE("new_expense", R.string.push_channel_new_expense, R.string.push_channel_new_expense_description),
    PAYMENT_MARKED(
        "payment_marked",
        R.string.push_channel_payment_marked,
        R.string.push_channel_payment_marked_description,
    ),
    SETTLE_REMINDERS(
        "settle_reminders",
        R.string.push_channel_settle_reminders,
        R.string.push_channel_settle_reminders_description,
    ),
    INVITES("invites", R.string.push_channel_invites, R.string.push_channel_invites_description),
}
