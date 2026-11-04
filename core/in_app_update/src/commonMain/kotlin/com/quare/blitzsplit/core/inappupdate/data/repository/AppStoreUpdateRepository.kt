package com.quare.blitzsplit.core.inappupdate.data.repository

import co.touchlab.kermit.Logger
import com.quare.blitzsplit.core.inappupdate.data.AppStoreStorefront
import com.quare.blitzsplit.core.inappupdate.data.VersionComparator
import com.quare.blitzsplit.core.inappupdate.data.dto.AppStoreLookupDto
import com.quare.blitzsplit.core.inappupdate.data.dto.AppStoreLookupResultDto
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateDownloadState
import com.quare.blitzsplit.core.inappupdate.domain.repository.AppUpdateRepository
import com.quare.blitzsplit.core.utils.suspendRunCatching
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.json.Json
import kotlin.random.Random

/*
 * Why: the App Store has no in-app update, so its listing's version is compared with the installed one, and the sheet
 * the shared code opens sends there. The lookup goes by bundle id, which needs no store id and finds nothing until the
 * app is published: no update then. The iPhone and Mac apps are one purchase with one listing, whose version the
 * lookup reports for both; they ship together from version.properties.
 */
internal class AppStoreUpdateRepository(
    private val httpClient: HttpClient,
    private val storefront: AppStoreStorefront,
    private val installedVersion: String,
    private val getRegionCode: () -> String?,
) : AppUpdateRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun check(): UpdateAvailability {
        val listing = findListing().getOrElse { throwable ->
            Logger.w(throwable) { "Failed to look the app up in the App Store" }
            return UpdateAvailability.CheckFailed
        }
        val storeVersion = listing?.version
        val trackViewUrl = listing?.trackViewUrl
        if (storeVersion == null || trackViewUrl == null) return UpdateAvailability.NotAvailable
        return if (VersionComparator.compare(storeVersion, installedVersion) > 0) {
            UpdateAvailability.Available(
                versionName = storeVersion,
                storeUrl = storefront.toStoreUrl(trackViewUrl),
            )
        } else {
            UpdateAvailability.NotAvailable
        }
    }

    override suspend fun startUpdate() {
        // Why: the sheet opens the App Store itself.
    }

    override fun observeDownload(): Flow<UpdateDownloadState> = emptyFlow()

    override suspend fun completeInstall() {
        // Why: the App Store installs on its own.
    }

    private suspend fun findListing(): Result<AppStoreLookupResultDto?> {
        val regionCode = getRegionCode() ?: return fetchListing(country = null)
        val regional = fetchListing(country = regionCode)
        /*
         * Why: the device's region can differ from the App Store account's storefront, so an app the regional store
         * doesn't list is looked up again in the default one.
         */
        val isMissingFromRegionalStore = regional.isSuccess && regional.getOrNull() == null
        return if (isMissingFromRegionalStore) fetchListing(country = null) else regional
    }

    private suspend fun fetchListing(country: String?): Result<AppStoreLookupResultDto?> = suspendRunCatching {
        val response = httpClient.get(LOOKUP_URL) {
            parameter("bundleId", BUNDLE_ID)
            country?.let { parameter("country", it) }
            /*
             * Why: Apple's CDN keeps serving the previous version of a repeated lookup for hours after a release, and
             * its max-age keeps it in the device's URL cache for a day; a unique query skips both.
             */
            parameter("t", Random.nextLong())
        }
        check(response.status.isSuccess()) { "App Store lookup answered ${response.status.value}" }
        json.decodeFromString<AppStoreLookupDto>(response.bodyAsText()).results?.firstOrNull()
    }

    private companion object {
        const val LOOKUP_URL = "https://itunes.apple.com/lookup"

        // Why: the legacy Blitz Split's id, which this app keeps since it replaces it in the stores (iosApp/project.yml).
        const val BUNDLE_ID = "com.quare.blitzsplit"
    }
}
