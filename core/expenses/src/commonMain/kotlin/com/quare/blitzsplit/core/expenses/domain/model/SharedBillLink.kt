package com.quare.blitzsplit.core.expenses.domain.model

// Why: a bill's link, blitzsplit.com/c/<code>, which the web opens as the bill (7.3h); the apps leave it to the browser.
object SharedBillLink {
    private const val BASE_URL = "https://blitzsplit.com/c/"

    // Why: any host, as an invite's, since the web app serves the path wherever it is hosted.
    private val linkPattern = Regex("^https?://[^/?#]+/c/([A-Za-z0-9]{8})/?(\\?[^#]*)?(#.*)?$")

    fun of(code: String): String = BASE_URL + code

    // Why: "Compartilhar" on a comment of the link's page, which opens the page scrolled to it.
    fun of(
        code: String,
        commentId: String,
    ): String = "${of(code)}?${BillCommentLink.COMMENT_PARAMETER}=$commentId"

    // Why: "blitzsplit.com/c/7Hk2pQ8x", the link as the sheet shows it.
    fun displayOf(link: String): String = link.substringAfter("://")

    // Why: the code of a bill's link, null for any other URL.
    fun codeOf(url: String): String? = linkPattern.matchEntire(url.trim())?.groupValues?.get(1)

    // Why: the comment a bill's link was shared at, null for none or any other URL.
    fun commentIdOf(url: String): String? = linkPattern
        .matchEntire(url.trim())
        ?.groupValues
        ?.get(2)
        ?.let(BillCommentLink::commentIdOf)
}
