package com.quare.blitzsplit.wear.push

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.quare.blitzsplit.core.activity.domain.usecase.MarkActivityRead
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/*
 * Why: "Received" on "Elisa marked that she paid you" (15.7). The payment is already recorded by whoever paid (no
 * one confirms a payment), so recording it again would count it twice: the watch takes it as seen, the activity read
 * and the notification gone (Pierre's call, November 2026).
 */
class ReceivedReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val markActivityRead: MarkActivityRead by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val tag = intent.getStringExtra(EXTRA_TAG) ?: return
        NotificationManagerCompat.from(context).cancel(tag, 0)
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            // Why: the broadcast ends whatever happens, or the system waits on it until its timeout.
            try {
                markActivityRead(eventId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val EXTRA_TAG = "tag"
        private const val EXTRA_EVENT_ID = "event_id"

        fun intentOf(
            context: Context,
            tag: String,
            eventId: String?,
        ): PendingIntent = PendingIntent.getBroadcast(
            context,
            tag.hashCode(),
            Intent(context, ReceivedReceiver::class.java)
                .putExtra(EXTRA_TAG, tag)
                .putExtra(EXTRA_EVENT_ID, eventId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
