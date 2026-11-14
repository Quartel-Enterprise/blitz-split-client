package com.quare.blitzsplit.core.model

/*
 * Why: "blitzsplit.com/g/7Hk2pQ8x", a link as the screens print it: without its scheme nor what its query carries
 * for whoever opens it (who shared it, the app's language).
 */
object DisplayLink {
    fun of(link: String): String = link.substringAfter("://").substringBefore("?")
}
