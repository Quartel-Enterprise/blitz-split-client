package com.quare.blitzsplit.wear.link

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.quare.blitzsplit.core.watch.link.WatchLink
import com.quare.blitzsplit.feature.watchpairing.presentation.model.PhoneReply
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/*
 * Why: the watch's side of the Data Layer: the pairing code to the phone that has the app (its capability), and the
 * phone's answer back. Only a phone in reach takes it; anything else is out of reach (15.11c).
 */
object PhoneLink {
    /*
     * Why: true when a phone took the message; its answer comes later through startListeningForReplies. A cancel still
     * cancels: :core:utils' suspendRunCatching can't be inlined here, since the app compiles for an older JVM.
     */
    @Suppress("ktlint:quare:suspend-run-catching")
    suspend fun sendCode(
        context: Context,
        code: String,
    ): Boolean = try {
        val phones = Wearable
            .getCapabilityClient(context)
            .getCapability(WatchLink.PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .await()
            .nodes
        // Why: the phone paired by Bluetooth first, which is the one nearby, then any other with the app.
        val phone = phones.firstOrNull { it.isNearby } ?: phones.firstOrNull() ?: return false
        Wearable
            .getMessageClient(context)
            .sendMessage(phone.id, WatchLink.PAIR_PATH, code.encodeToByteArray())
            .await()
        true
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        false
    }

    // Why: the phone's answer while the connect screens show; returns what stops listening.
    fun startListeningForReplies(
        context: Context,
        onReply: (PhoneReply) -> Unit,
    ): () -> Unit {
        val client = Wearable.getMessageClient(context)
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path == WatchLink.PAIR_REPLY_PATH) onReply(replyOf(event.data.decodeToString()))
        }
        client.addListener(listener)
        return { client.removeListener(listener) }
    }

    private fun replyOf(text: String): PhoneReply = when (text) {
        WatchLink.REPLY_APPROVED -> PhoneReply.APPROVED
        WatchLink.REPLY_LOCAL_ONLY -> PhoneReply.LOCAL_ONLY
        else -> PhoneReply.UNREACHABLE
    }
}
