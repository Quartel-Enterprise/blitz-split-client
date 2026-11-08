package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptRequestDto
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SupabaseReceiptRemoteDataSourceTest {
    private lateinit var dataSource: SupabaseReceiptRemoteDataSource
    private var sentTimeout: HttpTimeoutConfig? = null
    private val request = ReadReceiptRequestDto(
        image = "aW1hZ2U=",
        currency = "BRL",
        categories = emptyList(),
        terms = emptyList(),
    )

    @Test
    fun `GIVEN a receipt Claude takes long to read WHEN reading it THEN waits a minute for the answer`() = runTest {
        // Given
        prepareScenario()

        // When
        dataSource.readReceipt(request)

        // Then
        val timeout = assertNotNull(sentTimeout)
        assertEquals(60_000, timeout.requestTimeoutMillis)
        assertEquals(60_000, timeout.socketTimeoutMillis)
    }

    @Test
    fun `GIVEN the reader answers WHEN reading a receipt THEN returns what it read`() = runTest {
        // Given
        prepareScenario()

        // When
        val reading = dataSource.readReceipt(request)

        // Then
        assertEquals(6420L, reading?.totalMinor)
    }

    private fun prepareScenario() {
        val engine = MockEngine { request ->
            sentTimeout = request.getCapabilityOrNull(HttpTimeoutCapability)
            respond(
                content = """{"totalMinor":6420,"items":[],"servicePercent":0}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val supabase = createSupabaseClient(
            supabaseUrl = "https://example.supabase.co",
            supabaseKey = "key",
        ) {
            httpEngine = engine
            install(Functions)
        }
        dataSource = SupabaseReceiptRemoteDataSource(supabase.functions)
    }
}
