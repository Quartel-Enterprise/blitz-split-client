package com.quare.blitzsplit.core.legal.data.datasource

import com.quare.blitzsplit.core.model.LegalPage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

/*
 * Why: blitzsplit.com/legal/<page>.<language>.md, the Markdown the site's own legal pages are generated from. The site
 * answers 404 for a language it doesn't write the legal texts in, and 304 when the ETag sent is the current text's.
 */
internal class SiteLegalRemoteDataSource(
    private val httpClient: HttpClient,
) : LegalRemoteDataSource {
    override suspend fun fetchMarkdown(
        page: LegalPage,
        language: String,
        etag: String?,
    ): LegalRemoteAnswer {
        val response = httpClient.get("$BASE_URL/${page.slug}.$language.md") {
            etag?.let { header(HttpHeaders.IfNoneMatch, it) }
        }
        return when {
            response.status == HttpStatusCode.NotModified -> LegalRemoteAnswer.Unchanged

            response.status == HttpStatusCode.NotFound -> LegalRemoteAnswer.Missing

            else -> {
                check(response.status.isSuccess()) { "The legal page answered ${response.status.value}" }
                LegalRemoteAnswer.Text(
                    markdown = response.bodyAsText(),
                    etag = response.headers[HttpHeaders.ETag],
                )
            }
        }
    }

    private companion object {
        const val BASE_URL = "https://blitzsplit.com/legal"
    }
}
