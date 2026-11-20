package com.quare.blitzsplit.android.feature.notificationsettings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
import com.quare.blitzsplit.android.push.PushChannelIds
import com.quare.blitzsplit.core.model.NotificationType

/*
 * Why: one channel per row of Notifications (4.8), named with its own words, so the system's settings silence the
 * same groups of notifications the app's Push toggles do, and a row tells when its channel is silenced. The ids of the
 * first four stay as when they were the only toggles, so a channel the person already silenced stays silenced.
 */
enum class PushChannel(
    val id: String,
    val type: NotificationType,
    @param:StringRes val nameRes: Int,
    @param:StringRes val descriptionRes: Int,
    // Why: the channel these notifications came on before they had their own, whose silence they keep.
    private val formerChannelId: String? = null,
) {
    NEW_EXPENSE(
        PushChannelIds.NEW_EXPENSE,
        NotificationType.BILLS,
        R.string.notification_settings_bills,
        R.string.notification_settings_bills_detail,
    ),
    PAYMENT_MARKED(
        PushChannelIds.PAYMENT_MARKED,
        NotificationType.PAYMENTS,
        R.string.notification_settings_payments,
        R.string.notification_settings_payments_detail,
    ),
    COMMENTS(
        PushChannelIds.COMMENTS,
        NotificationType.COMMENTS,
        R.string.notification_settings_comments,
        R.string.notification_settings_comments_detail,
        formerChannelId = PushChannelIds.NEW_EXPENSE,
    ),
    INVITES(
        PushChannelIds.INVITES,
        NotificationType.PEOPLE_AND_GROUPS,
        R.string.notification_settings_people_and_groups,
        R.string.notification_settings_people_and_groups_detail,
    ),
    PAYMENT_REQUESTS(
        PushChannelIds.PAYMENT_REQUESTS,
        NotificationType.PAYMENT_REQUESTS,
        R.string.notification_settings_payment_requests,
        R.string.notification_settings_payment_requests_detail,
        formerChannelId = PushChannelIds.SETTLE_REMINDERS,
    ),
    SETTLE_REMINDERS(
        PushChannelIds.SETTLE_REMINDERS,
        NotificationType.BALANCE_REMINDER,
        R.string.notification_settings_balance_reminder,
        R.string.notification_settings_balance_reminder_detail,
    ),
    ;

    companion object {
        val ids: List<String> = entries.map { it.id }

        fun of(type: NotificationType): PushChannel = entries.first { it.type == type }

        // Why: the rows of these channels, the ones the system silenced.
        fun typesOf(channelIds: Set<String>): Set<NotificationType> =
            entries.filter { it.id in channelIds }.mapTo(mutableSetOf()) { it.type }

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
