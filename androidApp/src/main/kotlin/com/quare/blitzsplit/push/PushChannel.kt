package com.quare.blitzsplit.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
import com.quare.blitzsplit.android.feature.notificationsettings.R

/*
 * Why: one channel per row of Notifications (4.8), named with its own words, so the system's settings silence the
 * same groups of notifications the app's Push toggles do. The ids of the first four stay as when they were the only
 * toggles, so a channel the person already silenced stays silenced.
 */
enum class PushChannel(
    val id: String,
    @param:StringRes val nameRes: Int,
    @param:StringRes val descriptionRes: Int,
    // Why: the channel these notifications came on before they had their own, whose silence they keep.
    private val formerChannelId: String? = null,
) {
    NEW_EXPENSE(
        "new_expense",
        R.string.notification_settings_bills,
        R.string.notification_settings_bills_detail,
    ),
    PAYMENT_MARKED(
        "payment_marked",
        R.string.notification_settings_payments,
        R.string.notification_settings_payments_detail,
    ),
    COMMENTS(
        "comments",
        R.string.notification_settings_comments,
        R.string.notification_settings_comments_detail,
        formerChannelId = "new_expense",
    ),
    INVITES(
        "invites",
        R.string.notification_settings_people_and_groups,
        R.string.notification_settings_people_and_groups_detail,
    ),
    PAYMENT_REQUESTS(
        "payment_requests",
        R.string.notification_settings_payment_requests,
        R.string.notification_settings_payment_requests_detail,
        formerChannelId = "settle_reminders",
    ),
    SETTLE_REMINDERS(
        "settle_reminders",
        R.string.notification_settings_balance_reminder,
        R.string.notification_settings_balance_reminder_detail,
    ),
    ;

    companion object {
        /*
         * Why: all at once, so the system's settings list them before the first notification of each. Again on every
         * start, which renames the ones an older version named.
         */
        fun createAll(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(
                entries.map { channel ->
                    NotificationChannel(
                        channel.id,
                        context.getString(channel.nameRes),
                        channel.findInitialImportance(manager),
                    ).apply { description = context.getString(channel.descriptionRes) }
                },
            )
        }
    }

    /*
     * Why: a channel new to this device starts silenced when the person had silenced the one its notifications came
     * on before; one that exists keeps what the person set, since the system ignores a new importance for it.
     */
    private fun findInitialImportance(manager: NotificationManager): Int {
        val former = formerChannelId?.let(manager::getNotificationChannel)
        val isNew = manager.getNotificationChannel(id) == null
        return if (isNew && former?.importance == NotificationManager.IMPORTANCE_NONE) {
            NotificationManager.IMPORTANCE_NONE
        } else {
            NotificationManager.IMPORTANCE_DEFAULT
        }
    }
}
