package com.quare.blitzsplit.core.provider.supabase

import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.encodedPath

private const val REST_PATH = "/rest/v1/"
private const val FUNCTIONS_PATH = "/rest/v1/rpc/"
private const val BEARER_PREFIX = "Bearer "
private val anonymousFunctions = setOf("get_invite", "get_shared_bill", "unregister_push_device")

// Why: supabase-kt rethrows it as an HttpRequestException, which the sync counts as a connection failure.
internal class UserSessionRequiredException(
    val path: String,
) : IllegalStateException("The database was asked without a signed in user: $path")

/*
 * Why: supabase-kt sends a request with the publishable key while the session has no usable token, which happens when
 * a token that expired in the background can't be refreshed yet (no internet right after the app comes back). Row
 * level security then answers a read with no rows instead of an error, and a sync took that as everything gone from
 * the server: the groups and their bills left the device until the next sync brought them back. A database function
 * that reads the user's rows (first_download_groups, pending_guest_merges) answers the same way. So every request to
 * the database fails here without a user, but the functions the API grants to anon (an invite's preview), and the
 * device keeps what it has. The paths follow the project's URL, which a self-hosted server may serve under a prefix.
 */
internal fun createSignedInRestAccess(
    supabaseUrl: String,
    publishableKey: String,
): ClientPlugin<Unit> = createClientPlugin("SignedInRestAccess") {
    val root = Url(supabaseUrl).encodedPath.trimEnd('/')
    val restPath = root + REST_PATH
    val anonymousPaths = anonymousFunctions.map { name -> root + FUNCTIONS_PATH + name }.toSet()
    onRequest { request, _ ->
        val path = request.url.encodedPath
        val needsUser = path.startsWith(restPath) && path.trimEnd('/') !in anonymousPaths
        val token = request.headers[HttpHeaders.Authorization]?.removePrefix(BEARER_PREFIX)
        if (needsUser && (token.isNullOrBlank() || token == publishableKey)) throw UserSessionRequiredException(path)
    }
}
