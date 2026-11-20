package com.quare.blitzsplit.wear.link

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.quare.blitzsplit.core.watch.domain.usecase.EndWatchSessionFromPhone
import com.quare.blitzsplit.core.watch.link.WatchLink
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

/*
 * Why: the phone signed out (15.13), so the watch signs out and erases its data when it is that account's, even with
 * the app closed. The pairing's replies are only listened to while the connect screens show (PhoneLink).
 */
class WatchLinkListenerService : WearableListenerService() {
    private val endWatchSessionFromPhone: EndWatchSessionFromPhone by inject()

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WatchLink.SIGNED_OUT_PATH) return
        runBlocking { endWatchSessionFromPhone(event.data.decodeToString()) }
    }
}
