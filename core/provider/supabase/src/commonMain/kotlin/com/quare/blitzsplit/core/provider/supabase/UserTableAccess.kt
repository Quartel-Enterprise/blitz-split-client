package com.quare.blitzsplit.core.provider.supabase

import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.encodedPath

private const val TABLES_PATH = "/rest/v1/"
private const val FUNCTIONS_PATH = "/rest/v1/rpc/"
private const val BEARER_PREFIX = "Bearer "

// Why: supabase-kt rethrows it as an HttpRequestException, which the sync counts as a connection failure.
internal class UserSessionRequiredException(
    val path: String,
) : IllegalStateException("A table was asked for without a signed in user: $path")

/*
 * Why: supabase-kt sends a request with the publishable key while the session has no usable token, which happens when
 * a token that expired in the background can't be refreshed yet (no internet right after the app comes back). Row
 * level security then answers a table's read with no rows instead of an error, and a sync took that as everything
 * gone from the server: the groups and their bills left the device until the next sync brought them back. No table
 * is ever read or written without a user, so such a request fails here and the device keeps what it has. The
 * database functions are left alone: some answer without an account (an invite's preview), and those that need one
 * fail on the server. The paths follow the project's URL, which a self-hosted server may serve under a prefix.
 */
internal fun createUserTableAccess(
    supabaseUrl: String,
    publishableKey: String,
): ClientPlugin<Unit> = createClientPlugin("UserTableAccess") {
    val root = Url(supabaseUrl).encodedPath.trimEnd('/')
    val tablesPath = root + TABLES_PATH
    val functionsPath = root + FUNCTIONS_PATH
    onRequest { request, _ ->
        val path = request.url.encodedPath
        val isTable = path.startsWith(tablesPath) && !path.startsWith(functionsPath)
        val token = request.headers[HttpHeaders.Authorization]?.removePrefix(BEARER_PREFIX)
        if (isTable && (token.isNullOrBlank() || token == publishableKey)) throw UserSessionRequiredException(path)
    }
}
