package com.quare.blitzsplit.core.groups.domain.model

/*
 * Why: the group's link, whoever opens it picks who they are in the group (flow 3), and a member's personal link,
 * blitzsplit.com/g/<group code>/<member code>, which joins straight in as that member. A personal link ends in
 * ?from=<member code> of whoever shared it, so the invite says who sent it.
 */
object InviteLink {
    private const val BASE_URL = "https://blitzsplit.com/g/"
    private const val FROM = "from"

    /*
     * Why: any host, since only the app's own domains reach the apps, and the web app serves the path wherever it is
     * hosted (borarachar.com, which sends its links to blitzsplit.com, a preview, a local server).
     */
    private val linkPattern = Regex("^https?://[^/?#]+/g/([A-Za-z0-9]{8})(?:/([A-Za-z0-9]{6}))?/?(\\?[^#]*)?(#.*)?$")
    private val fromPattern = Regex("(?:^|&)$FROM=([A-Za-z0-9]{6})(?:&|$)")

    fun of(inviteCode: String): String = BASE_URL + inviteCode

    // Why: without the code of whoever shares it, the invite names nobody: "Você entra como Carla".
    fun personalOf(
        inviteCode: String,
        memberCode: String,
        inviterCode: String?,
    ): String = BASE_URL + inviteCode + "/" + memberCode + inviterCode?.let { "?$FROM=$it" }.orEmpty()

    // Why: "blitzsplit.com/g/7Hk2pQ8x/c3Rt9z", a link as the screens show it, without whoever shares it.
    fun displayOf(link: String): String = link.substringAfter("://").substringBefore("?")

    // Why: what a link opens, null for any other URL, like the browser sign in's callback.
    fun targetOf(url: String): InviteTarget? {
        val match = linkPattern.matchEntire(url.trim()) ?: return null
        val (code, memberCode, query) = match.destructured
        val personalCode = memberCode.ifEmpty { null }
        return InviteTarget(
            code = code,
            memberCode = personalCode,
            // Why: only a personal link says who shared it.
            inviterCode = personalCode?.let {
                fromPattern.find(query.removePrefix("?"))?.groupValues?.get(1)
            },
        )
    }
}
