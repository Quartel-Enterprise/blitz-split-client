package com.quare.blitzsplit.core.settings.data

import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.provider.room.entity.DeviceSettingsEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import com.quare.blitzsplit.core.settings.data.datasource.DeviceSettingsLocalDataSource
import com.quare.blitzsplit.core.settings.data.repository.DeviceSettingsRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceSettingsRepositoryImplTest {
    private lateinit var local: FakeDeviceSettingsLocalDataSource
    private lateinit var repository: DeviceSettingsRepositoryImpl
    private lateinit var observed: MutableList<ThemePreference>

    @Test
    fun `GIVEN a fresh install with no theme stored WHEN observing the theme THEN follows the system`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        val themes = observed.toList()

        // Then
        assertEquals(listOf(ThemePreference.SYSTEM), themes)
    }

    @Test
    fun `GIVEN dark picked on this device WHEN observing the theme THEN is dark`() = runTest {
        // Given
        prepareScenario(stored = settings(theme = "dark"))

        // When
        val themes = observed.toList()

        // Then
        assertEquals(listOf(ThemePreference.DARK), themes)
    }

    @Test
    fun `GIVEN light picked on this device WHEN observing the theme THEN is light`() = runTest {
        // Given
        prepareScenario(stored = settings(theme = "light"))

        // When
        val themes = observed.toList()

        // Then
        assertEquals(listOf(ThemePreference.LIGHT), themes)
    }

    @Test
    fun `GIVEN a theme code this version does not know WHEN observing the theme THEN follows the system`() = runTest {
        // Given
        prepareScenario(stored = settings(theme = "sepia"))

        // When
        val themes = observed.toList()

        // Then
        assertEquals(listOf(ThemePreference.SYSTEM), themes)
    }

    @Test
    fun `GIVEN a fresh install WHEN picking dark THEN stores it on the device row`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        repository.updateTheme(ThemePreference.DARK)

        // Then
        assertEquals(listOf(settings(theme = "dark")), local.rows.values.toList())
    }

    @Test
    fun `GIVEN the theme being observed WHEN picking dark THEN observers see dark`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        repository.updateTheme(ThemePreference.DARK)

        // Then
        assertEquals(listOf(ThemePreference.SYSTEM, ThemePreference.DARK), observed)
    }

    @Test
    fun `GIVEN light already picked WHEN picking dark THEN replaces light in the single device row`() = runTest {
        // Given
        prepareScenario(stored = settings(theme = "light"))

        // When
        repository.updateTheme(ThemePreference.DARK)

        // Then
        assertEquals(listOf(settings(theme = "dark")), local.rows.values.toList())
        assertEquals(listOf(ThemePreference.LIGHT, ThemePreference.DARK), observed)
    }

    @Test
    fun `GIVEN dark stored WHEN the device row is emitted again unchanged THEN observers are not notified again`() =
        runTest {
            // Given
            prepareScenario(stored = settings(theme = "dark"))

            // When
            local.emitAgain()

            // Then
            assertEquals(listOf(ThemePreference.DARK), observed)
        }

    @Test
    fun `GIVEN a fresh install WHEN picking system THEN observers are not notified again`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        repository.updateTheme(ThemePreference.SYSTEM)

        // Then
        assertEquals(listOf(ThemePreference.SYSTEM), observed)
        assertEquals(listOf(settings(theme = "system")), local.rows.values.toList())
    }

    @Test
    fun `GIVEN a theme code this version does not know WHEN picking system THEN observers are not notified again`() =
        runTest {
            // Given
            prepareScenario(stored = settings(theme = "sepia"))

            // When
            repository.updateTheme(ThemePreference.SYSTEM)

            // Then
            assertEquals(listOf(ThemePreference.SYSTEM), observed)
        }

    private fun settings(theme: String): DeviceSettingsEntity = DeviceSettingsEntity(id = SINGLE_ROW_ID, theme = theme)

    private fun TestScope.prepareScenario(stored: DeviceSettingsEntity?) {
        local = FakeDeviceSettingsLocalDataSource(stored)
        repository = DeviceSettingsRepositoryImpl(local = local)
        observed = mutableListOf()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeTheme().collect(observed::add)
        }
    }
}

/*
 * Like the DAO: rows keyed by their primary key, observed through the single device row, and
 * re-emitted on every write, even one that leaves the row as it was.
 */
private class FakeDeviceSettingsLocalDataSource(
    stored: DeviceSettingsEntity?,
) : DeviceSettingsLocalDataSource {
    val rows = mutableMapOf<String, DeviceSettingsEntity>()
    private val emissions = MutableSharedFlow<DeviceSettingsEntity?>(replay = 1)

    init {
        stored?.let { rows[it.id] = it }
        emissions.tryEmit(stored)
    }

    override fun observe(): Flow<DeviceSettingsEntity?> = emissions

    override suspend fun upsert(entity: DeviceSettingsEntity) {
        rows[entity.id] = entity
        emitAgain()
    }

    suspend fun emitAgain() {
        emissions.emit(rows[SINGLE_ROW_ID])
    }
}
