package com.quare.blitzsplit.core.profile.domain.model

/*
 * Why: an account's profile link, blitzsplit.com/u/<code> (Profile's "Share profile"), which opens the person's profile
 * for whoever has it: their contact profile when the two share a group, their name and photo otherwise.
 */
object ProfileLink {
    // Why: as long as the server's codes (profiles.share_code); this device makes the account's own (ProfileRepository).
    const val CODE_LENGTH = 10

    private const val BASE_URL = "https://blitzsplit.com/u/"

    /*
     * Why: any host, since only the app's own domains reach the apps, and the web app serves the path wherever it is
     * hosted (borarachar.com, which sends its links to blitzsplit.com, a preview, a local server).
     */
    private val linkPattern = Regex("^https?://[^/?#]+/u/([A-Za-z0-9]{$CODE_LENGTH})/?(\\?[^#]*)?(#.*)?$")
    private val codePattern = Regex("^[A-Za-z0-9]{$CODE_LENGTH}$")

    fun of(code: String): String = BASE_URL + code

    // Why: "blitzsplit.com/u/7Hk2pQ8xZa", the link as the screens show it.
    fun displayOf(link: String): String = link.substringAfter("://")

    // Why: the code a link opens, null for any other URL.
    fun codeOf(url: String): String? = linkPattern.matchEntire(url.trim())?.groupValues?.get(1)

    fun isCode(code: String): Boolean = codePattern.matches(code)
}
