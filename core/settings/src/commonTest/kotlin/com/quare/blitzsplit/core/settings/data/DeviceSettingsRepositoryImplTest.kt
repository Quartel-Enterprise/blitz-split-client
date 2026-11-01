package com.quare.blitzsplit.core.settings.data

import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.model.GroupsLayout
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
    private lateinit var observedLayouts: MutableList<GroupsLayout>

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

    @Test
    fun `GIVEN nothing picked on this device WHEN observing the layout THEN is the list with the standard grid`() =
        runTest {
            // Given
            prepareScenario(stored = null)

            // When
            val layouts = observedLayouts.toList()

            // Then
            assertEquals(listOf(GroupsLayout.initial), layouts)
        }

    @Test
    fun `GIVEN the large grid picked on this device WHEN observing the layout THEN is the large grid`() = runTest {
        // Given
        prepareScenario(
            stored = settings(
                theme = "dark",
                groupsDisplay = "grid",
                groupGridSize = "large",
            ),
        )

        // When
        val layouts = observedLayouts.toList()

        // Then
        assertEquals(listOf(GroupsLayout(GroupsDisplay.GRID, GroupGridSize.LARGE)), layouts)
    }

    @Test
    fun `GIVEN layout codes this version does not know WHEN observing the layout THEN each falls back on its own`() =
        runTest {
            // Given
            prepareScenario(
                stored = settings(
                    theme = "light",
                    groupsDisplay = "map",
                    groupGridSize = "compact",
                ),
            )

            // When
            val layouts = observedLayouts.toList()

            // Then
            assertEquals(listOf(GroupsLayout(GroupsDisplay.LIST, GroupGridSize.COMPACT)), layouts)
        }

    @Test
    fun `GIVEN dark picked WHEN picking the compact grid THEN stores it and keeps dark`() = runTest {
        // Given
        prepareScenario(stored = settings(theme = "dark"))

        // When
        repository.updateGroupsLayout(GroupsLayout(GroupsDisplay.GRID, GroupGridSize.COMPACT))

        // Then
        assertEquals(
            listOf(
                settings(
                    theme = "dark",
                    groupsDisplay = "grid",
                    groupGridSize = "compact",
                ),
            ),
            local.rows.values.toList(),
        )
        assertEquals(listOf(ThemePreference.DARK), observed)
        assertEquals(
            listOf(GroupsLayout.initial, GroupsLayout(GroupsDisplay.GRID, GroupGridSize.COMPACT)),
            observedLayouts,
        )
    }

    @Test
    fun `GIVEN the grid picked WHEN picking dark THEN keeps the grid`() = runTest {
        // Given
        prepareScenario(
            stored = settings(
                theme = "system",
                groupsDisplay = "grid",
                groupGridSize = "large",
            ),
        )

        // When
        repository.updateTheme(ThemePreference.DARK)

        // Then
        assertEquals(
            listOf(
                settings(
                    theme = "dark",
                    groupsDisplay = "grid",
                    groupGridSize = "large",
                ),
            ),
            local.rows.values.toList(),
        )
        assertEquals(listOf(GroupsLayout(GroupsDisplay.GRID, GroupGridSize.LARGE)), observedLayouts)
    }

    private fun settings(
        theme: String,
        groupsDisplay: String? = null,
        groupGridSize: String? = null,
    ): DeviceSettingsEntity = DeviceSettingsEntity(
        id = SINGLE_ROW_ID,
        theme = theme,
        groupsDisplay = groupsDisplay,
        groupGridSize = groupGridSize,
    )

    private fun TestScope.prepareScenario(stored: DeviceSettingsEntity?) {
        local = FakeDeviceSettingsLocalDataSource(stored)
        repository = DeviceSettingsRepositoryImpl(local = local)
        observed = mutableListOf()
        observedLayouts = mutableListOf()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeTheme().collect(observed::add)
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeGroupsLayout().collect(observedLayouts::add)
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

    override suspend fun update(transform: (DeviceSettingsEntity) -> DeviceSettingsEntity) {
        val current = rows[SINGLE_ROW_ID] ?: DeviceSettingsEntity(
            id = SINGLE_ROW_ID,
            theme = "system",
            groupsDisplay = null,
            groupGridSize = null,
        )
        rows[SINGLE_ROW_ID] = transform(current)
        emitAgain()
    }

    suspend fun emitAgain() {
        emissions.emit(rows[SINGLE_ROW_ID])
    }
}
