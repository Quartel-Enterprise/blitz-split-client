package com.quare.blitzsplit.core.releasenotes.data.datasource

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

/*
 * Why: blitzsplit.com/release-notes/<language>.json, every version's notes in one language, which the site's own
 * "What's new" page is generated with. The site answers 404 for a language it doesn't write them in, and 304 when the
 * ETag sent is the current feed's. The web asks from another origin, where a browser hides the ETag from the app: it
 * never sends one back, and its daily check leans on the browser's own cache instead.
 */
internal class SiteReleaseNotesRemoteDataSource(
    private val httpClient: HttpClient,
) : ReleaseNotesRemoteDataSource {
    override suspend fun fetchFeed(
        language: String,
        etag: String?,
    ): ReleaseNotesRemoteAnswer {
        val response = httpClient.get("$BASE_URL/$language.json") {
            etag?.let { header(HttpHeaders.IfNoneMatch, it) }
        }
        return when {
            response.status == HttpStatusCode.NotModified -> ReleaseNotesRemoteAnswer.Unchanged

            response.status == HttpStatusCode.NotFound -> ReleaseNotesRemoteAnswer.Missing

            else -> {
                check(response.status.isSuccess()) { "The release notes answered ${response.status.value}" }
                ReleaseNotesRemoteAnswer.Feed(
                    json = response.bodyAsText(),
                    etag = response.headers[HttpHeaders.ETag],
                )
            }
        }
    }

    private companion object {
        const val BASE_URL = "https://blitzsplit.com/release-notes"
    }
}
