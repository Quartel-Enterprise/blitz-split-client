package com.quare.blitzsplit.core.sync.domain.model

/*
 * Why: "3 de 7 grupos" (5.1d): how many groups came down whole, out of those the server said were coming; groupCount is
 * null until it answered, and after a relaunch midway, when only the account knows a download is still due.
 */
data class FirstDownloadProgress(
    val arrivedGroupCount: Int,
    val groupCount: Int?,
    // Why: offline nothing comes down, so the screens say the rest waits for the internet instead of counting.
    val isWaitingForInternet: Boolean,
)
