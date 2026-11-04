package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: a comment's link ("Compartilhar" on a comment), blitzsplit.com/b/<bill>?comentario=<comment>, which the apps
 * and the web open as the bill's Conta, scrolled to the comment, for someone in its group.
 */
object BillCommentLink {
    private const val BASE_URL = "https://blitzsplit.com/b/"
    const val COMMENT_PARAMETER = "comentario"

    // Why: any host, as an invite's, since the web app serves the path wherever it is hosted.
    private val linkPattern = Regex("^https?://[^/?#]+/b/([0-9A-Fa-f-]{36})/?(\\?[^#]*)?(#.*)?$")

    fun of(
        expenseId: String,
        commentId: String,
    ): String = "$BASE_URL$expenseId?$COMMENT_PARAMETER=$commentId"

    // Why: the bill and the comment of a comment's link, null for any other URL; a link without its comment opens the bill.
    fun targetOf(url: String): BillCommentTarget? {
        val match = linkPattern.matchEntire(url.trim()) ?: return null
        return BillCommentTarget(
            expenseId = match.groupValues[1].lowercase(),
            commentId = commentIdOf(match.groupValues[2]),
        )
    }

    // Why: "?comentario=<id>" among a link's query, also on a bill's link (7.3h).
    internal fun commentIdOf(query: String): String? = query
        .removePrefix("?")
        .split('&')
        .mapNotNull { pair -> pair.split('=', limit = 2).takeIf { it.size == 2 } }
        .firstOrNull { (name, _) -> name == COMMENT_PARAMETER }
        ?.get(1)
        ?.takeIf { it.isNotEmpty() }
}

data class BillCommentTarget(
    val expenseId: String,
    val commentId: String?,
)
