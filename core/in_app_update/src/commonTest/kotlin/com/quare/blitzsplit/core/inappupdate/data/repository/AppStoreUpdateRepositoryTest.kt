package com.quare.blitzsplit.core.inappupdate.data.repository

import com.quare.blitzsplit.core.inappupdate.data.AppStoreStorefront
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AppStoreUpdateRepositoryTest {
    private lateinit var repository: AppStoreUpdateRepository
    private val requests = mutableListOf<Url>()

    @Test
    fun `GIVEN a newer version on the iPhone App Store WHEN checking THEN offers it with its store page`() = runTest {
        // Given
        prepareScenario(defaultStore = StoreAnswer.Body(listing(version = "2.1.0")))

        // When
        val availability = repository.check()

        // Then
        assertEquals(
            UpdateAvailability.Available(
                versionName = "2.1.0",
                storeUrl = APP_PAGE,
            ),
            availability,
        )
    }

    @Test
    fun `GIVEN a newer version on the Mac App Store WHEN checking THEN its page opens in the Mac App Store`() =
        runTest {
            // Given
            prepareScenario(
                storefront = AppStoreStorefront.MACOS,
                defaultStore = StoreAnswer.Body(listing(version = "2.1.0")),
            )

            // When
            val availability = repository.check()

            // Then
            assertEquals(
                UpdateAvailability.Available(
                    versionName = "2.1.0",
                    storeUrl = "macappstore://apps.apple.com/us/app/blitz-split/id1234567890",
                ),
                availability,
            )
        }

    @Test
    fun `GIVEN the store listing the installed version WHEN checking THEN there is no update`() = runTest {
        // Given
        prepareScenario(
            installedVersion = "2.0.0",
            defaultStore = StoreAnswer.Body(listing(version = "2.0.0")),
        )

        // When
        val availability = repository.check()

        // Then
        assertEquals(UpdateAvailability.NotAvailable, availability)
    }

    @Test
    fun `GIVEN the store listing an older version than installed WHEN checking THEN there is no update`() = runTest {
        // Given
        prepareScenario(
            installedVersion = "2.1.0",
            defaultStore = StoreAnswer.Body(listing(version = "2.0.9")),
        )

        // When
        val availability = repository.check()

        // Then
        assertEquals(UpdateAvailability.NotAvailable, availability)
    }

    @Test
    fun `GIVEN the store listing 2 10 0 and 2 9 1 installed WHEN checking THEN offers 2 10 0`() = runTest {
        // Given
        prepareScenario(
            installedVersion = "2.9.1",
            defaultStore = StoreAnswer.Body(listing(version = "2.10.0")),
        )

        // When
        val availability = repository.check()

        // Then
        assertEquals(
            UpdateAvailability.Available(
                versionName = "2.10.0",
                storeUrl = APP_PAGE,
            ),
            availability,
        )
    }

    @Test
    fun `GIVEN the store listing 2 0 and 2 0 0 installed WHEN checking THEN there is no update`() = runTest {
        // Given
        prepareScenario(
            installedVersion = "2.0.0",
            defaultStore = StoreAnswer.Body(listing(version = "2.0")),
        )

        // When
        val availability = repository.check()

        // Then
        assertEquals(UpdateAvailability.NotAvailable, availability)
    }

    @Test
    fun `GIVEN the app not published yet WHEN checking THEN there is no update`() = runTest {
        // Given
        prepareScenario(defaultStore = StoreAnswer.Body(NO_RESULTS))

        // When
        val availability = repository.check()

        // Then
        assertEquals(UpdateAvailability.NotAvailable, availability)
    }

    @Test
    fun `GIVEN an answer without results WHEN checking THEN there is no update`() = runTest {
        // Given
        prepareScenario(defaultStore = StoreAnswer.Body("""{"resultCount":0}"""))

        // When
        val availability = repository.check()

        // Then
        assertEquals(UpdateAvailability.NotAvailable, availability)
    }

    @Test
    fun `GIVEN a listing without its version WHEN checking THEN there is no update`() = runTest {
        // Given
        prepareScenario(defaultStore = StoreAnswer.Body(listing(version = null)))

        // When
        val availability = repository.check()

        // Then
        assertEquals(UpdateAvailability.NotAvailable, availability)
    }

    @Test
    fun `GIVEN a listing without its store page WHEN checking THEN there is no update`() = runTest {
        // Given
        prepareScenario(
            defaultStore = StoreAnswer.Body(
                listing(
                    version = "9.0.0",
                    trackViewUrl = null,
                ),
            ),
        )

        // When
        val availability = repository.check()

        // Then
        assertEquals(UpdateAvailability.NotAvailable, availability)
    }

    @Test
    fun `GIVEN a listing with fields the app does not read WHEN checking THEN offers the newer version`() = runTest {
        // Given
        val body =
            """
            {"resultCount":1,"results":[{"trackName":"Blitz Split","price":0.0,"genres":["Finance"],
            "version":"2.1.0","trackViewUrl":"$APP_PAGE","releaseNotes":"Bug fixes"}]}
            """.trimIndent()
        prepareScenario(defaultStore = StoreAnswer.Body(body))

        // When
        val availability = repository.check()

        // Then
        assertEquals(
            UpdateAvailability.Available(
                versionName = "2.1.0",
                storeUrl = APP_PAGE,
            ),
            availability,
        )
    }

    @Test
    fun `GIVEN the store answering a server error WHEN checking THEN the check failed`() = runTest {
        // Given
        prepareScenario(defaultStore = StoreAnswer.Body(listing(version = "2.1.0"), HttpStatusCode.InternalServerError))

        // When
        val availability = repository.check()

        // Then
        assertEquals(UpdateAvailability.CheckFailed, availability)
    }

    @Test
    fun `GIVEN the store answering something that is not JSON WHEN checking THEN the check failed`() = runTest {
        // Given
        prepareScenario(defaultStore = StoreAnswer.Body("<html>Service Unavailable</html>"))

        // When
        val availability = repository.check()

        // Then
        assertEquals(UpdateAvailability.CheckFailed, availability)
    }

    @Test
    fun `GIVEN no connection to the store WHEN checking THEN the check failed`() = runTest {
        // Given
        prepareScenario(defaultStore = StoreAnswer.Unreachable)

        // When
        val availability = repository.check()

        // Then
        assertEquals(UpdateAvailability.CheckFailed, availability)
    }

    @Test
    fun `GIVEN the iPhone in a known region WHEN checking THEN looks the app up in that region uncached`() = runTest {
        // Given
        prepareScenario(
            regionCode = "BR",
            regionalStore = StoreAnswer.Body(listing(version = "2.1.0")),
        )

        // When
        repository.check()

        // Then
        val request = requests.single()
        assertEquals("https://itunes.apple.com/lookup", request.toString().substringBefore("?"))
        assertEquals("com.quare.blitzsplit", request.parameters["bundleId"])
        assertEquals("BR", request.parameters["country"])
        assertNull(request.parameters["entity"])
        assertNotNull(request.parameters["t"])
    }

    @Test
    fun `GIVEN the Mac WHEN checking THEN looks up the same listing as the iPhone`() = runTest {
        // Given
        prepareScenario(
            storefront = AppStoreStorefront.MACOS,
            defaultStore = StoreAnswer.Body(listing(version = "2.1.0")),
        )

        // When
        repository.check()

        // Then
        val request = requests.single()
        assertEquals("com.quare.blitzsplit", request.parameters["bundleId"])
        assertNull(request.parameters["entity"])
    }

    @Test
    fun `GIVEN no known region WHEN checking THEN looks up once without a country`() = runTest {
        // Given
        prepareScenario(
            regionCode = null,
            defaultStore = StoreAnswer.Body(NO_RESULTS),
        )

        // When
        repository.check()

        // Then
        assertEquals(1, requests.size)
        assertFalse(requests.single().parameters.contains("country"))
    }

    @Test
    fun `GIVEN two checks WHEN checking again THEN each lookup has its own cache buster`() = runTest {
        // Given
        prepareScenario(defaultStore = StoreAnswer.Body(listing(version = "2.1.0")))
        repository.check()

        // When
        repository.check()

        // Then
        assertEquals(2, requests.size)
        assertNotEquals(requests[0].parameters["t"], requests[1].parameters["t"])
    }

    @Test
    fun `GIVEN the regional store listing the app WHEN checking THEN offers its version alone`() = runTest {
        // Given
        prepareScenario(
            regionCode = "BR",
            regionalStore = StoreAnswer.Body(listing(version = "2.1.0")),
            defaultStore = StoreAnswer.Body(listing(version = "3.0.0")),
        )

        // When
        val availability = repository.check()

        // Then
        assertEquals(
            UpdateAvailability.Available(
                versionName = "2.1.0",
                storeUrl = APP_PAGE,
            ),
            availability,
        )
        assertEquals(1, requests.size)
    }

    @Test
    fun `GIVEN the regional store not listing the app WHEN checking THEN offers the default store version`() = runTest {
        // Given
        prepareScenario(
            regionCode = "BR",
            regionalStore = StoreAnswer.Body(NO_RESULTS),
            defaultStore = StoreAnswer.Body(listing(version = "2.1.0")),
        )

        // When
        val availability = repository.check()

        // Then
        assertEquals(
            UpdateAvailability.Available(
                versionName = "2.1.0",
                storeUrl = APP_PAGE,
            ),
            availability,
        )
        assertEquals(listOf<String?>("BR", null), requests.map { it.parameters["country"] })
    }

    @Test
    fun `GIVEN no store listing the app WHEN checking THEN there is no update`() = runTest {
        // Given
        prepareScenario(
            regionCode = "BR",
            regionalStore = StoreAnswer.Body(NO_RESULTS),
            defaultStore = StoreAnswer.Body(NO_RESULTS),
        )

        // When
        val availability = repository.check()

        // Then
        assertEquals(UpdateAvailability.NotAvailable, availability)
        assertEquals(2, requests.size)
    }

    @Test
    fun `GIVEN only the default store lists it but fails WHEN checking THEN the check failed`() = runTest {
        // Given
        prepareScenario(
            regionCode = "BR",
            regionalStore = StoreAnswer.Body(NO_RESULTS),
            defaultStore = StoreAnswer.Unreachable,
        )

        // When
        val availability = repository.check()

        // Then
        assertEquals(UpdateAvailability.CheckFailed, availability)
    }

    @Test
    fun `GIVEN the regional lookup failing WHEN checking THEN fails without asking the default store`() = runTest {
        // Given
        prepareScenario(
            regionCode = "BR",
            regionalStore = StoreAnswer.Body(NO_RESULTS, HttpStatusCode.ServiceUnavailable),
            defaultStore = StoreAnswer.Body(listing(version = "2.1.0")),
        )

        // When
        val availability = repository.check()

        // Then
        assertEquals(UpdateAvailability.CheckFailed, availability)
        assertEquals(1, requests.size)
    }

    private fun listing(
        version: String?,
        trackViewUrl: String? = APP_PAGE,
    ): String {
        val fields = listOfNotNull(
            version?.let { "\"version\":\"$it\"" },
            trackViewUrl?.let { "\"trackViewUrl\":\"$it\"" },
        ).joinToString(",")
        return """{"resultCount":1,"results":[{$fields}]}"""
    }

    private fun prepareScenario(
        storefront: AppStoreStorefront = AppStoreStorefront.IOS,
        installedVersion: String = "2.0.0",
        regionCode: String? = null,
        regionalStore: StoreAnswer = StoreAnswer.Body(NO_RESULTS),
        defaultStore: StoreAnswer = StoreAnswer.Body(NO_RESULTS),
    ) {
        val engine = MockEngine { request ->
            requests += request.url
            val answer = if (request.url.parameters.contains("country")) regionalStore else defaultStore
            when (answer) {
                is StoreAnswer.Body -> respond(
                    content = answer.json,
                    status = answer.status,
                    headers = headersOf(HttpHeaders.ContentType, "text/javascript; charset=utf-8"),
                )

                StoreAnswer.Unreachable -> throw IllegalStateException("No route to itunes.apple.com")
            }
        }
        repository = AppStoreUpdateRepository(
            httpClient = HttpClient(engine),
            storefront = storefront,
            installedVersion = installedVersion,
            getRegionCode = { regionCode },
        )
    }

    private companion object {
        const val APP_PAGE = "https://apps.apple.com/us/app/blitz-split/id1234567890"
        const val NO_RESULTS = """{"resultCount":0,"results":[]}"""
    }
}

// Why: what the iTunes lookup answers, per storefront the request asks for.
private sealed interface StoreAnswer {
    class Body(
        val json: String,
        val status: HttpStatusCode = HttpStatusCode.OK,
    ) : StoreAnswer

    data object Unreachable : StoreAnswer
}
