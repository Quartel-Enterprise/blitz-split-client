package com.quare.blitzsplit.core.releasenotes.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.ReleaseNotesFeedDao
import com.quare.blitzsplit.core.provider.room.entity.ReleaseNotesFeedEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoomReleaseNotesLocalDataSourceTest {
    private lateinit var dao: FakeReleaseNotesFeedDao
    private lateinit var dataSource: RoomReleaseNotesLocalDataSource
    private val observed = mutableListOf<String?>()

    @Test
    fun `GIVEN nothing saved WHEN observing the feed in Portuguese THEN there is none`() = runTest {
        // Given
        prepareScenario()

        // When
        val json = dataSource.observeFeed("pt").first()

        // Then
        assertNull(json)
    }

    @Test
    fun `GIVEN the feed saved in Portuguese WHEN observing it in Portuguese THEN gives the saved JSON`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    language = "pt",
                    json = "novidades",
                ),
            ),
        )

        // When
        val json = dataSource.observeFeed("pt").first()

        // Then
        assertEquals("novidades", json)
    }

    @Test
    fun `GIVEN only the Portuguese feed saved WHEN observing it in Spanish THEN there is none`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(language = "pt")))

        // When
        val json = dataSource.observeFeed("es").first()

        // Then
        assertNull(json)
    }

    @Test
    fun `GIVEN the Portuguese feed is being observed with nothing saved WHEN saving it THEN the JSON follows`() =
        runTest {
            // Given
            prepareScenario(observing = "pt")

            // When
            dataSource.saveFeed(
                language = "pt",
                json = "novidades",
                etag = null,
                checkedAt = 1_000L,
            )
            runCurrent()

            // Then
            assertEquals(listOf(null, "novidades"), observed)
        }

    @Test
    fun `GIVEN nothing saved WHEN reading the saved feed THEN there is none`() = runTest {
        // Given
        prepareScenario()

        // When
        val saved = dataSource.getSavedFeed("pt")

        // Then
        assertNull(saved)
    }

    @Test
    fun `GIVEN the feed saved with an ETag WHEN reading it THEN gives its ETag and check time`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    language = "pt",
                    etag = "\"abc\"",
                    checkedAt = 1_000L,
                ),
            ),
        )

        // When
        val saved = dataSource.getSavedFeed("pt")

        // Then
        assertEquals(
            SavedReleaseNotesFeed(
                etag = "\"abc\"",
                checkedAt = 1_000L,
            ),
            saved,
        )
    }

    @Test
    fun `GIVEN the feed saved without an ETag WHEN reading it THEN gives no ETag and its check time`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    language = "pt",
                    etag = null,
                    checkedAt = 1_000L,
                ),
            ),
        )

        // When
        val saved = dataSource.getSavedFeed("pt")

        // Then
        assertEquals(
            SavedReleaseNotesFeed(
                etag = null,
                checkedAt = 1_000L,
            ),
            saved,
        )
    }

    @Test
    fun `GIVEN only the Portuguese feed saved WHEN reading the saved Spanish feed THEN there is none`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(language = "pt")))

        // When
        val saved = dataSource.getSavedFeed("es")

        // Then
        assertNull(saved)
    }

    @Test
    fun `GIVEN nothing saved WHEN saving the Portuguese feed THEN keeps its JSON ETag and check time`() = runTest {
        // Given
        prepareScenario()

        // When
        dataSource.saveFeed(
            language = "pt",
            json = "novidades",
            etag = "\"abc\"",
            checkedAt = 1_000L,
        )

        // Then
        assertEquals(
            listOf(
                ReleaseNotesFeedEntity(
                    language = "pt",
                    json = "novidades",
                    etag = "\"abc\"",
                    checkedAt = 1_000L,
                ),
            ),
            dao.rows.value,
        )
    }

    @Test
    fun `GIVEN the Portuguese and Spanish feeds saved WHEN saving the Portuguese one again THEN replaces only it`() =
        runTest {
            // Given
            val spanish = entity(
                language = "es",
                json = "novedades",
                etag = "\"es\"",
                checkedAt = 500L,
            )
            prepareScenario(
                stored = listOf(
                    entity(
                        language = "pt",
                        json = "old",
                        etag = "\"old\"",
                        checkedAt = 1_000L,
                    ),
                    spanish,
                ),
            )

            // When
            dataSource.saveFeed(
                language = "pt",
                json = "new",
                etag = null,
                checkedAt = 2_000L,
            )

            // Then
            assertEquals(
                setOf(
                    spanish,
                    ReleaseNotesFeedEntity(
                        language = "pt",
                        json = "new",
                        etag = null,
                        checkedAt = 2_000L,
                    ),
                ),
                dao.rows.value.toSet(),
            )
            assertEquals(2, dao.rows.value.size)
        }

    @Test
    fun `GIVEN the Portuguese and Spanish feeds saved WHEN marking the Portuguese checked THEN only its time moves`() =
        runTest {
            // Given
            val spanish = entity(
                language = "es",
                json = "novedades",
                etag = "\"es\"",
                checkedAt = 500L,
            )
            prepareScenario(
                stored = listOf(
                    entity(
                        language = "pt",
                        json = "novidades",
                        etag = "\"abc\"",
                        checkedAt = 1_000L,
                    ),
                    spanish,
                ),
            )

            // When
            dataSource.markChecked(
                language = "pt",
                checkedAt = 2_000L,
            )

            // Then
            assertEquals(
                listOf(
                    ReleaseNotesFeedEntity(
                        language = "pt",
                        json = "novidades",
                        etag = "\"abc\"",
                        checkedAt = 2_000L,
                    ),
                    spanish,
                ),
                dao.rows.value,
            )
        }

    private fun entity(
        language: String,
        json: String = "{}",
        etag: String? = null,
        checkedAt: Long = 0L,
    ): ReleaseNotesFeedEntity = ReleaseNotesFeedEntity(
        language = language,
        json = json,
        etag = etag,
        checkedAt = checkedAt,
    )

    private fun TestScope.prepareScenario(
        stored: List<ReleaseNotesFeedEntity> = emptyList(),
        observing: String? = null,
    ) {
        dao = FakeReleaseNotesFeedDao(stored)
        dataSource = RoomReleaseNotesLocalDataSource(dao)
        observing?.let { language ->
            backgroundScope.launch { dataSource.observeFeed(language).collect(observed::add) }
            runCurrent()
        }
    }
}

private class FakeReleaseNotesFeedDao(
    stored: List<ReleaseNotesFeedEntity>,
) : ReleaseNotesFeedDao {
    val rows = MutableStateFlow(stored)

    override fun observe(language: String): Flow<ReleaseNotesFeedEntity?> = rows.map { all ->
        all.firstOrNull { it.language == language }
    }

    override suspend fun find(language: String): ReleaseNotesFeedEntity? = rows.value.firstOrNull {
        it.language == language
    }

    override suspend fun upsert(entity: ReleaseNotesFeedEntity) {
        rows.update { all -> all.filterNot { it.language == entity.language } + entity }
    }

    override suspend fun markChecked(
        language: String,
        checkedAt: Long,
    ) {
        rows.update { all -> all.map { if (it.language == language) it.copy(checkedAt = checkedAt) else it } }
    }
}
