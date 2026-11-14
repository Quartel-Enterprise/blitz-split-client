package com.quare.blitzsplit.core.legal.data.datasource

import com.quare.blitzsplit.core.model.LegalPage
import com.quare.blitzsplit.core.provider.room.dao.LegalDocumentDao
import com.quare.blitzsplit.core.provider.room.entity.LegalDocumentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoomLegalLocalDataSourceTest {
    private lateinit var dao: FakeLegalDocumentDao
    private lateinit var dataSource: RoomLegalLocalDataSource

    @Test
    fun `GIVEN nothing saved WHEN observing the terms THEN there is no text`() = runTest {
        // Given
        prepareScenario()

        // When
        val markdown = dataSource.observeMarkdown(LegalPage.TERMS_OF_USE, "pt").first()

        // Then
        assertNull(markdown)
    }

    @Test
    fun `GIVEN the terms saved in Portuguese WHEN observing them in Portuguese THEN gives the saved text`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                LegalDocumentEntity(
                    page = "terms",
                    language = "pt",
                    markdown = "Termos",
                    etag = null,
                    checkedAt = null,
                ),
            ),
        )

        // When
        val markdown = dataSource.observeMarkdown(LegalPage.TERMS_OF_USE, "pt").first()

        // Then
        assertEquals("Termos", markdown)
    }

    @Test
    fun `GIVEN a privacy policy already saved WHEN saving it again THEN replaces it under its slug and language`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    LegalDocumentEntity(
                        page = "privacy",
                        language = "pt",
                        markdown = "Old",
                        etag = null,
                        checkedAt = null,
                    ),
                ),
            )

            // When
            dataSource.saveMarkdown(
                page = LegalPage.PRIVACY_POLICY,
                language = "pt",
                markdown = "New",
                etag = "\"new\"",
                checkedAt = 1_000L,
            )

            // Then
            assertEquals(
                listOf(
                    LegalDocumentEntity(
                        page = "privacy",
                        language = "pt",
                        markdown = "New",
                        etag = "\"new\"",
                        checkedAt = 1_000L,
                    ),
                ),
                dao.rows.value,
            )
        }

    @Test
    fun `GIVEN nothing saved WHEN reading the saved privacy policy THEN there is none`() = runTest {
        // Given
        prepareScenario()

        // When
        val saved = dataSource.getSavedText(LegalPage.PRIVACY_POLICY, "pt")

        // Then
        assertNull(saved)
    }

    @Test
    fun `GIVEN the terms saved with an ETag WHEN reading them THEN gives their ETag and check time`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                LegalDocumentEntity(
                    page = "terms",
                    language = "pt",
                    markdown = "Termos",
                    etag = "\"abc\"",
                    checkedAt = 1_000L,
                ),
            ),
        )

        // When
        val saved = dataSource.getSavedText(LegalPage.TERMS_OF_USE, "pt")

        // Then
        assertEquals(
            SavedLegalText(
                etag = "\"abc\"",
                checkedAt = 1_000L,
            ),
            saved,
        )
    }

    @Test
    fun `GIVEN the terms saved WHEN marking them checked THEN only their check time moves`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                LegalDocumentEntity(
                    page = "terms",
                    language = "pt",
                    markdown = "Termos",
                    etag = "\"abc\"",
                    checkedAt = 1_000L,
                ),
            ),
        )

        // When
        dataSource.markChecked(
            page = LegalPage.TERMS_OF_USE,
            language = "pt",
            checkedAt = 2_000L,
        )

        // Then
        assertEquals(
            listOf(
                LegalDocumentEntity(
                    page = "terms",
                    language = "pt",
                    markdown = "Termos",
                    etag = "\"abc\"",
                    checkedAt = 2_000L,
                ),
            ),
            dao.rows.value,
        )
    }

    private fun prepareScenario(stored: List<LegalDocumentEntity> = emptyList()) {
        dao = FakeLegalDocumentDao(stored)
        dataSource = RoomLegalLocalDataSource(dao)
    }
}

private class FakeLegalDocumentDao(
    stored: List<LegalDocumentEntity>,
) : LegalDocumentDao {
    val rows = MutableStateFlow(stored)

    override fun observe(
        page: String,
        language: String,
    ): Flow<LegalDocumentEntity?> = rows.map { all -> all.firstOrNull { it.page == page && it.language == language } }

    override suspend fun find(
        page: String,
        language: String,
    ): LegalDocumentEntity? = rows.value.firstOrNull { it.page == page && it.language == language }

    override suspend fun upsert(entity: LegalDocumentEntity) {
        rows.update { all -> all.filterNot { it.page == entity.page && it.language == entity.language } + entity }
    }

    override suspend fun markChecked(
        page: String,
        language: String,
        checkedAt: Long,
    ) {
        rows.update { all ->
            all.map { if (it.page == page && it.language == language) it.copy(checkedAt = checkedAt) else it }
        }
    }
}
