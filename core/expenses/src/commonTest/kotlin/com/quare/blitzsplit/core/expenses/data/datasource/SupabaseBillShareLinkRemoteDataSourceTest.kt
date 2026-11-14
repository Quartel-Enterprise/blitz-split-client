package com.quare.blitzsplit.core.expenses.data.datasource

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SupabaseBillShareLinkRemoteDataSourceTest {
    private lateinit var dataSource: SupabaseBillShareLinkRemoteDataSource

    @Test
    fun `GIVEN a link only the people in the bill open WHEN reading its bill THEN reads the status alone`() = runTest {
        // Given
        prepareScenario(answer = """{"status": "members_only"}""")

        // When
        val dto = dataSource.fetchSharedBill(CODE)

        // Then
        assertEquals("members_only", dto.status)
        assertNull(dto.bill)
        assertNull(dto.groupName)
    }

    @Test
    fun `GIVEN a code no link has WHEN reading its bill THEN reads the status alone`() = runTest {
        // Given
        prepareScenario(answer = """{"status": "not_found"}""")

        // When
        val dto = dataSource.fetchSharedBill(CODE)

        // Then
        assertEquals("not_found", dto.status)
        assertNull(dto.comments)
    }

    @Test
    fun `GIVEN a bill that is gone WHEN turning its link on THEN reads the status alone`() = runTest {
        // Given
        prepareScenario(answer = """{"status": "not_found"}""")

        // When
        val dto = dataSource.updateLink(
            expenseId = EXPENSE_ID,
            visibility = "anyone",
        )

        // Then
        assertEquals("not_found", dto.status)
        assertNull(dto.code)
    }

    @Test
    fun `GIVEN a bill that is gone WHEN turning its link off THEN reads the status alone`() = runTest {
        // Given
        prepareScenario(answer = """{"status": "not_found"}""")

        // When
        val dto = dataSource.disableLink(EXPENSE_ID)

        // Then
        assertEquals("not_found", dto.status)
        assertNull(dto.visibility)
    }

    @Test
    fun `GIVEN a link turned on WHEN the server answers it THEN reads its code and who opens it`() = runTest {
        // Given
        prepareScenario(answer = """{"status": "ok", "code": "7Hk2pQ8x", "visibility": "members"}""")

        // When
        val dto = dataSource.updateLink(
            expenseId = EXPENSE_ID,
            visibility = "members",
        )

        // Then
        assertEquals("7Hk2pQ8x", dto.code)
        assertEquals("members", dto.visibility)
    }

    private fun prepareScenario(answer: String) {
        val engine = MockEngine {
            respond(
                content = answer,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val supabase = createSupabaseClient(
            supabaseUrl = "https://example.supabase.co",
            supabaseKey = "key",
        ) {
            httpEngine = engine
            install(Postgrest)
        }
        dataSource = SupabaseBillShareLinkRemoteDataSource(supabase.postgrest)
    }

    private companion object {
        const val CODE = "7Hk2pQ8x"
        const val EXPENSE_ID = "12191cca-1266-43d0-8c27-a668b9c2e4a8"
    }
}
