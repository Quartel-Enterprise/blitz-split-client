package com.quare.blitzsplit.core.watch.domain.model

import kotlin.time.Duration

/*
 * Why: the code a watch asked the server for (15.12, "K7P 4QX"), which a nearby phone approves on its own or a person
 * types in Profile › Connect watch; the secret only the watch knows, which it polls with until the code is approved.
 */
data class WatchPairingCode(
    val code: String,
    val secret: String,
    // Why: epoch millis; the server keeps a code for 5 minutes.
    val expiresAt: Long,
    val pollInterval: Duration,
)
