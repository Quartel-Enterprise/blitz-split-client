package com.quare.blitzsplit.watch

import android.content.Context
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.quare.blitzsplit.core.watch.domain.usecase.ApproveNearbyWatch
import com.quare.blitzsplit.core.watch.link.WatchLink
import com.quare.blitzsplit.shared.settings.LegacyAppLocale
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.koin.android.ext.android.inject

/*
 * Why: a nearby watch hands its pairing code over the Data Layer, and the phone approves it with its own session, with
 * no confirmation (Pierre's call: the channel only exists with a watch paired to this phone, which needs it
 * unlocked), then shows "Pixel Watch connected to your account" (15.15). The watch's own poll brings its session; the
 * reply only tells it to stop waiting, or that this profile is local only (15.11b).
 */
class WatchPairingListenerService : WearableListenerService() {
    private val approveNearbyWatch: ApproveNearbyWatch by inject()

    // Why: before Android 13 the notice it posts is in Profile › Language's pick, as the screens are.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LegacyAppLocale.wrap(newBase))
    }

    // Why: runs off the main thread, one message at a time, so the approval may block it until the server answers.
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WatchLink.PAIR_PATH) return
        val code = event.data.decodeToString()
        runBlocking {
            val reply = approveNearbyWatch(code)
            attemptOrNull {
                Wearable
                    .getMessageClient(this@WatchPairingListenerService)
                    .sendMessage(event.sourceNodeId, WatchLink.PAIR_REPLY_PATH, reply.encodeToByteArray())
                    .await()
            }
            if (reply == WatchLink.REPLY_APPROVED) {
                WatchConnectedNotice.show(
                    this@WatchPairingListenerService,
                    watchNameOf(event.sourceNodeId),
                )
            }
        }
    }

    // Why: the name the watch has on the system ("Pixel Watch 3"), as the notice says; null when it can't be read.
    private suspend fun watchNameOf(nodeId: String): String? = attemptOrNull {
        Wearable
            .getNodeClient(this)
            .connectedNodes
            .await()
            .firstOrNull { it.id == nodeId }
            ?.displayName
    }
}
