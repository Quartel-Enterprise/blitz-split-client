package com.quare.blitzsplit.core.sync.domain.model

/*
 * Why: "3 de 7 grupos" (5.1d): how many groups came down whole, out of those the server said were coming; groupCount is
 * null until it answered.
 */
data class FirstDownloadProgress(
    val arrivedGroupCount: Int,
    val groupCount: Int?,
)
