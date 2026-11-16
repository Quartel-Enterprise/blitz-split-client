package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.model.AccountExpiredException
import com.quare.blitzsplit.core.account.domain.model.SessionRefreshingException
import com.quare.blitzsplit.core.provider.crashlytics.ReportableFailure
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.ktor.client.plugins.HttpRequestTimeoutException

/*
 * Why: what a sync that failed online tells Crashlytics: each failure's kind and the server's codes, cause by cause,
 * never their messages, which hold the request's URL and PostgREST's details (ids, names, amounts).
 */
internal class SyncFailure(
    failure: Throwable,
) : Exception(codesOf(failure)),
    ReportableFailure

/*
 * Why: a request that never got an answer, a timeout or a connection that dropped, which the device may still call
 * online (supabase-kt wraps every failure in HttpRequestException except Ktor's request timeout, which it rethrows);
 * and a session whose token refresh got no answer the same way, so the sync didn't ask the server anything.
 */
internal fun Throwable.isConnectionFailure(): Boolean = getCauses().any { cause ->
    cause is HttpRequestException || cause is HttpRequestTimeoutException || cause is SessionRefreshingException
}

/*
 * Why: a sync that ran while the session waited for its refresh, as on coming back to the front with an expired token:
 * it asked the server nothing, so it is put off until the token comes back rather than failed.
 */
internal fun Throwable.isPutOffForSession(): Boolean = getCauses().any { cause -> cause is SessionRefreshingException }

/*
 * Why: what a failed sync doesn't report: a connection failure, and the sync of an account the server deleted for
 * inactivity, which only waits for the device to be erased.
 */
internal fun Throwable.isExpectedFailure(): Boolean =
    isConnectionFailure() || getCauses().any { cause -> cause is AccountExpiredException }

private fun codesOf(failure: Throwable): String = failure.getCauses().joinToString(separator = " ← ") { cause ->
    val name = cause::class.simpleName ?: "Throwable"
    when (cause) {
        is PostgrestRestException -> "$name ${cause.statusCode} ${cause.code}"
        is RestException -> "$name ${cause.statusCode}"
        else -> name
    }
}

private fun Throwable.getCauses(): Sequence<Throwable> = generateSequence(this) { it.cause }
