package com.quare.blitzsplit.core.account.data.datasource

import com.quare.blitzsplit.core.provider.supabase.AUTH_CALLBACK_HOST
import com.quare.blitzsplit.core.provider.supabase.AUTH_CALLBACK_SCHEME
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.ktor.http.Parameters
import io.ktor.http.URLDecodeException
import io.ktor.http.URLParserException
import io.ktor.http.Url
import io.ktor.http.parseQueryString

/*
 * Why: Supabase Auth redirects a failed OAuth flow with error, error_code and error_description in
 * both the query and the fragment, and a successful one with a code (PKCE, the native apps) or the
 * tokens in the fragment (implicit, the web). On the web the callback is the page itself, so only
 * that full shape counts there: a page with an unrelated ?code= or ?error= isn't a callback.
 */
internal object SupabaseCallbackUrl {
    fun read(url: String): BrowserCallback? = try {
        getCallbackOrNull(ParsedUrl(Url(url)))
    } catch (_: URLParserException) {
        null
    } catch (_: URLDecodeException) {
        null
    }

    private fun getCallbackOrNull(parsed: ParsedUrl): BrowserCallback? {
        val isAppCallback = parsed.url.protocol.name == AUTH_CALLBACK_SCHEME && parsed.url.host == AUTH_CALLBACK_HOST
        val hasError = parsed.firstOrNull(ERROR) != null &&
            (isAppCallback || parsed.firstOrNull(ERROR_DESCRIPTION) != null)
        val isIdentityTaken = parsed.firstOrNull(ERROR_CODE) == AuthErrorCode.IdentityAlreadyExists.value
        val hasCode = isAppCallback && parsed.firstOrNull(CODE) != null
        val hasTokens = parsed.fragment[ACCESS_TOKEN] != null && parsed.fragment[REFRESH_TOKEN] != null
        return when {
            hasError && isIdentityTaken -> BrowserCallback.IDENTITY_ALREADY_LINKED
            hasError -> BrowserCallback.FAILED
            hasCode || hasTokens -> BrowserCallback.SIGNED_IN
            else -> null
        }
    }

    private class ParsedUrl(
        val url: Url,
    ) {
        val fragment: Parameters = parseQueryString(url.fragment)

        fun firstOrNull(name: String): String? = url.parameters[name] ?: fragment[name]
    }

    private const val ERROR = "error"
    private const val ERROR_CODE = "error_code"
    private const val ERROR_DESCRIPTION = "error_description"
    private const val CODE = "code"
    private const val ACCESS_TOKEN = "access_token"
    private const val REFRESH_TOKEN = "refresh_token"
}
