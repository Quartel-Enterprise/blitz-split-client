package com.quare.blitzsplit.core.groups.domain.model

// Why: the group's link, whoever opens it picks who they are in the group (flow 3).
object InviteLink {
    private const val BASE_URL = "https://blitzsplit.com/g/"

    /*
     * Why: any host, since only the app's own domains reach the apps, and the web app serves the path wherever it is
     * hosted (borarachar.com, which sends its links to blitzsplit.com, a preview, a local server).
     */
    private val linkPattern = Regex("^https?://[^/?#]+/g/([A-Za-z0-9]{8})/?([?#].*)?$")

    fun of(inviteCode: String): String = BASE_URL + inviteCode

    // Why: the invite code of a link, null for any other URL, like the browser sign in's callback.
    fun codeOf(url: String): String? = linkPattern.matchEntire(url.trim())?.groupValues?.get(1)
}
