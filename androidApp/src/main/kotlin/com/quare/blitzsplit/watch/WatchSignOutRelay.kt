package com.quare.blitzsplit.watch

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.quare.blitzsplit.core.watch.domain.usecase.ObserveAccountSignOuts
import com.quare.blitzsplit.core.watch.link.WatchLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await

/*
 * Why: signing out on the phone tells each watch it can reach to sign out and erase its data too (15.13). A watch out
 * of reach finds out on its next sync, when the server refuses its session; a watch of another account ignores it.
 */
object WatchSignOutRelay {
    fun start(
        context: Context,
        observeAccountSignOuts: ObserveAccountSignOuts,
        scope: CoroutineScope,
    ) {
        val appContext = context.applicationContext
        observeAccountSignOuts()
            .onEach { userId -> attemptOrNull { tellWatches(appContext, userId) } }
            .launchIn(scope)
    }

    private suspend fun tellWatches(
        context: Context,
        userId: String,
    ) {
        val messages = Wearable.getMessageClient(context)
        // Why: only the watches that have the app, which declare its capability, not every paired device.
        Wearable
            .getCapabilityClient(context)
            .getCapability(WatchLink.WATCH_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .await()
            .nodes
            .forEach { node ->
                attemptOrNull {
                    messages
                        .sendMessage(
                            node.id,
                            WatchLink.SIGNED_OUT_PATH,
                            userId.encodeToByteArray(),
                        ).await()
                }
            }
    }
}
