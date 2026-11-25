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

    @Test
    fun `GIVEN the server before the frames came WHEN reading a shared bill THEN reads its photos with no frame`() =
        runTest {
            // Given
            prepareScenario(answer = sharedBillWith(attachment = """$ATTACHMENT_OPEN}"""))

            // When
            val dto = dataSource.fetchSharedBill(CODE)

            // Then
            val attachment = dto.attachments.orEmpty().single()
            assertEquals(
                listOf(null, null, null, null),
                listOf(attachment.frameLeft, attachment.frameTop, attachment.frameWidth, attachment.frameHeight),
            )
        }

    @Test
    fun `GIVEN a shared photo that keeps its whole image WHEN reading a shared bill THEN reads its frame`() = runTest {
        // Given
        prepareScenario(
            answer = sharedBillWith(
                attachment = """$ATTACHMENT_OPEN, "frame_left": -0.125, "frame_top": 0.25, """ +
                    """"frame_width": 1.25, "frame_height": 0.5}""",
            ),
        )

        // When
        val dto = dataSource.fetchSharedBill(CODE)

        // Then
        val attachment = dto.attachments.orEmpty().single()
        assertEquals(
            listOf(-0.125f, 0.25f, 1.25f, 0.5f),
            listOf(attachment.frameLeft, attachment.frameTop, attachment.frameWidth, attachment.frameHeight),
        )
    }

    private fun sharedBillWith(attachment: String): String =
        """{"status": "ok", "group_id": "group-1", "currency": "BRL", "attachments": [$attachment]}"""

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
        const val ATTACHMENT_OPEN =
            """{"id": "photo-1", "kind": "photo", "path": "group-1/bill-1/photo-1.jpg", "width": 1200, """ +
                """"height": 1600, "added_by_name": "Ana", "added_at": 2000"""
    }
}
