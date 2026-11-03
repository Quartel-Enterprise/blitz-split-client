package com.quare.blitzsplit.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
import com.quare.blitzsplit.android.feature.notificationsettings.R

/*
 * Why: one channel per toggle of Perfil › Notificações (4.8), named with its own words, so the system's settings
 * silence the same groups of notifications the app's toggles do.
 */
enum class PushChannel(
    val id: String,
    @param:StringRes val nameRes: Int,
    @param:StringRes val descriptionRes: Int,
) {
    NEW_EXPENSE(
        "new_expense",
        R.string.notification_settings_new_expense,
        R.string.notification_settings_new_expense_detail,
    ),
    PAYMENT_MARKED(
        "payment_marked",
        R.string.notification_settings_payment_marked,
        R.string.notification_settings_payment_marked_detail,
    ),
    SETTLE_REMINDERS(
        "settle_reminders",
        R.string.notification_settings_settle_reminders,
        R.string.notification_settings_settle_reminders_detail,
    ),
    INVITES("invites", R.string.notification_settings_invites, R.string.notification_settings_invites_detail),
    ;

    companion object {
        // Why: all at once, so the system's settings list them before the first notification of each.
        fun createAll(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(
                entries.map { channel ->
                    NotificationChannel(
                        channel.id,
                        context.getString(channel.nameRes),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply { description = context.getString(channel.descriptionRes) }
                },
            )
        }
    }
}
