package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.BillAttachmentDto
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class SupabaseBillAttachmentRemoteDataSourceTest {
    private lateinit var dataSource: SupabaseBillAttachmentRemoteDataSource
    private val sentBodies = mutableListOf<String>()
    private val frameKeys = listOf("frame_left", "frame_top", "frame_width", "frame_height")

    @Test
    fun `GIVEN an older row with null frame columns WHEN pulling the attachments THEN reads it with no frame`() =
        runTest {
            // Given
            prepareScenario(
                rows = """[$ROW_OPEN "frame_left": null, "frame_top": null, """ +
                    """"frame_width": null, "frame_height": null}]""",
            )

            // When
            val attachments = dataSource.fetchAttachments()

            // Then
            assertEquals(
                listOf(listOf(null, null, null, null)),
                attachments.map { listOf(it.frameLeft, it.frameTop, it.frameWidth, it.frameHeight) },
            )
        }

    @Test
    fun `GIVEN a photo that keeps its whole image on the server WHEN pulling the attachments THEN reads its frame`() =
        runTest {
            // Given
            prepareScenario(
                rows = """[$ROW_OPEN "frame_left": -0.125, "frame_top": 0.25, """ +
                    """"frame_width": 1.25, "frame_height": 0.5}]""",
            )

            // When
            val attachments = dataSource.fetchAttachments()

            // Then
            assertEquals(
                listOf(listOf(-0.125f, 0.25f, 1.25f, 0.5f)),
                attachments.map { listOf(it.frameLeft, it.frameTop, it.frameWidth, it.frameHeight) },
            )
        }

    @Test
    fun `GIVEN a photo that keeps its whole image WHEN pushing it THEN sends its frame under the frame keys`() =
        runTest {
            // Given
            prepareScenario(rows = "[]")
            val attachment = dto(
                frameLeft = -0.125f,
                frameTop = 0.25f,
                frameWidth = 1.25f,
                frameHeight = 0.5f,
            )

            // When
            dataSource.upsertAttachment(attachment)

            // Then
            val sent = sentRow()
            assertEquals(
                listOf("-0.125", "0.25", "1.25", "0.5"),
                frameKeys.map { key -> sent.getValue(key).jsonPrimitive.content },
            )
        }

    private fun sentRow(): JsonObject {
        val body: JsonElement = Json.parseToJsonElement(sentBodies.single())
        return if (body is JsonArray) body.single().jsonObject else body.jsonObject
    }

    private fun dto(
        frameLeft: Float?,
        frameTop: Float?,
        frameWidth: Float?,
        frameHeight: Float?,
    ): BillAttachmentDto = BillAttachmentDto(
        id = "photo-1",
        expenseId = "bill-1",
        groupId = "group-1",
        kind = "photo",
        addedByUserId = "user-ana",
        addedByMemberId = "member-ana",
        width = 1_600,
        height = 1_200,
        frameLeft = frameLeft,
        frameTop = frameTop,
        frameWidth = frameWidth,
        frameHeight = frameHeight,
        isDeleted = false,
        addedAt = 10L,
        updatedAt = 20L,
    )

    // Why: the rows answer the first page, then an empty page ends the paging.
    private fun prepareScenario(rows: String) {
        var pages = 0
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Get) {
                val page = if (pages++ == 0) rows else "[]"
                respond(
                    content = page,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                sentBodies += request.body.toByteArray().decodeToString()
                respond(
                    content = "",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        val supabase = createSupabaseClient(
            supabaseUrl = "https://example.supabase.co",
            supabaseKey = "key",
        ) {
            httpEngine = engine
            install(Postgrest)
            install(Storage)
        }
        dataSource = SupabaseBillAttachmentRemoteDataSource(
            postgrest = supabase.postgrest,
            storage = supabase.storage,
        )
    }

    private companion object {
        const val ROW_OPEN =
            """{"id": "photo-1", "expense_id": "bill-1", "group_id": "group-1", "kind": "photo", """ +
                """"added_by": "user-ana", "added_by_member_id": "member-ana", "width": 1600, "height": 1200, """ +
                """"is_deleted": false, "added_at": 10, "updated_at": 20,"""
    }
}
