package com.quare.blitzsplit.core.settings.data.datasource

import androidx.room3.Room
import androidx.room3.executeSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.DeviceSettingsEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Why: the data source over a real in-memory Room, since a whole-row REPLACE is what would lose the other settings.
class RoomDeviceSettingsLocalDataSourceTest {
    private lateinit var database: AppDatabase
    private lateinit var dataSource: RoomDeviceSettingsLocalDataSource

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `GIVEN a fresh install WHEN the grid is picked THEN the theme still follows the system`() = runTest {
        // Given
        prepareScenario()

        // When
        dataSource.update { it.copy(groupsDisplay = "grid") }

        // Then
        assertEquals(
            DeviceSettingsEntity(
                id = SINGLE_ROW_ID,
                theme = "system",
                groupsDisplay = "grid",
                groupGridSize = null,
                statsChartType = null,
                statsGrain = null,
                usesDynamicColors = false,
                showsBitcoinInSats = false,
                lastUpdatePromptAt = null,
                lastNotificationPromptAt = null,
                isAppBannerDismissed = false,
                sharesUsageData = true,
            ),
            dataSource.observe().first(),
        )
    }

    @Test
    fun `GIVEN the grid picked WHEN dark is picked THEN the grid stays`() = runTest {
        // Given
        prepareScenario()
        dataSource.update {
            it.copy(
                groupsDisplay = "grid",
                groupGridSize = "large",
            )
        }

        // When
        dataSource.update { it.copy(theme = "dark") }

        // Then
        assertEquals(
            DeviceSettingsEntity(
                id = SINGLE_ROW_ID,
                theme = "dark",
                groupsDisplay = "grid",
                groupGridSize = "large",
                statsChartType = null,
                statsGrain = null,
                usesDynamicColors = false,
                showsBitcoinInSats = false,
                lastUpdatePromptAt = null,
                lastNotificationPromptAt = null,
                isAppBannerDismissed = false,
                sharesUsageData = true,
            ),
            dataSource.observe().first(),
        )
    }

    @Test
    fun `GIVEN a fresh install WHEN Material You is turned on THEN the theme still follows the system`() = runTest {
        // Given
        prepareScenario()

        // When
        dataSource.update { it.copy(usesDynamicColors = true) }

        // Then
        assertEquals(
            DeviceSettingsEntity(
                id = SINGLE_ROW_ID,
                theme = "system",
                groupsDisplay = null,
                groupGridSize = null,
                statsChartType = null,
                statsGrain = null,
                usesDynamicColors = true,
                showsBitcoinInSats = false,
                lastUpdatePromptAt = null,
                lastNotificationPromptAt = null,
                isAppBannerDismissed = false,
                sharesUsageData = true,
            ),
            dataSource.observe().first(),
        )
    }

    @Test
    fun `GIVEN dark and the grid picked WHEN Material You is turned on THEN dark and the grid stay`() = runTest {
        // Given
        prepareScenario()
        dataSource.update {
            it.copy(
                theme = "dark",
                groupsDisplay = "grid",
                groupGridSize = "large",
            )
        }

        // When
        dataSource.update { it.copy(usesDynamicColors = true) }

        // Then
        assertEquals(
            DeviceSettingsEntity(
                id = SINGLE_ROW_ID,
                theme = "dark",
                groupsDisplay = "grid",
                groupGridSize = "large",
                statsChartType = null,
                statsGrain = null,
                usesDynamicColors = true,
                showsBitcoinInSats = false,
                lastUpdatePromptAt = null,
                lastNotificationPromptAt = null,
                isAppBannerDismissed = false,
                sharesUsageData = true,
            ),
            dataSource.observe().first(),
        )
    }

    @Test
    fun `GIVEN Material You on WHEN dark is picked THEN Material You stays on`() = runTest {
        // Given
        prepareScenario()
        dataSource.update { it.copy(usesDynamicColors = true) }

        // When
        dataSource.update { it.copy(theme = "dark") }

        // Then
        assertEquals(
            DeviceSettingsEntity(
                id = SINGLE_ROW_ID,
                theme = "dark",
                groupsDisplay = null,
                groupGridSize = null,
                statsChartType = null,
                statsGrain = null,
                usesDynamicColors = true,
                showsBitcoinInSats = false,
                lastUpdatePromptAt = null,
                lastNotificationPromptAt = null,
                isAppBannerDismissed = false,
                sharesUsageData = true,
            ),
            dataSource.observe().first(),
        )
    }

    @Test
    fun `GIVEN Material You on and dark picked WHEN Material You is turned off THEN dark stays`() = runTest {
        // Given
        prepareScenario()
        dataSource.update {
            it.copy(
                theme = "dark",
                usesDynamicColors = true,
                showsBitcoinInSats = false,
            )
        }

        // When
        dataSource.update { it.copy(usesDynamicColors = false) }

        // Then
        assertEquals(
            DeviceSettingsEntity(
                id = SINGLE_ROW_ID,
                theme = "dark",
                groupsDisplay = null,
                groupGridSize = null,
                statsChartType = null,
                statsGrain = null,
                usesDynamicColors = false,
                showsBitcoinInSats = false,
                lastUpdatePromptAt = null,
                lastNotificationPromptAt = null,
                isAppBannerDismissed = false,
                sharesUsageData = true,
            ),
            dataSource.observe().first(),
        )
    }

    @Test
    fun `GIVEN a fresh install WHEN usage data sharing is turned off THEN stores off and follows the system theme`() =
        runTest {
            // Given
            prepareScenario()

            // When
            dataSource.update { it.copy(sharesUsageData = false) }

            // Then
            assertEquals(
                DeviceSettingsEntity(
                    id = SINGLE_ROW_ID,
                    theme = "system",
                    groupsDisplay = null,
                    groupGridSize = null,
                    statsChartType = null,
                    statsGrain = null,
                    usesDynamicColors = false,
                    showsBitcoinInSats = false,
                    lastUpdatePromptAt = null,
                    lastNotificationPromptAt = null,
                    isAppBannerDismissed = false,
                    sharesUsageData = false,
                ),
                dataSource.observe().first(),
            )
        }

    @Test
    fun `GIVEN a fresh install WHEN dark is picked THEN usage data sharing stays on`() = runTest {
        // Given
        prepareScenario()

        // When
        dataSource.update { it.copy(theme = "dark") }

        // Then
        assertEquals(
            DeviceSettingsEntity(
                id = SINGLE_ROW_ID,
                theme = "dark",
                groupsDisplay = null,
                groupGridSize = null,
                statsChartType = null,
                statsGrain = null,
                usesDynamicColors = false,
                showsBitcoinInSats = false,
                lastUpdatePromptAt = null,
                lastNotificationPromptAt = null,
                isAppBannerDismissed = false,
                sharesUsageData = true,
            ),
            dataSource.observe().first(),
        )
    }

    @Test
    fun `GIVEN dark the grid Material You and sats WHEN sharing is turned off THEN they all stay`() = runTest {
        // Given
        prepareScenario()
        dataSource.update {
            it.copy(
                theme = "dark",
                groupsDisplay = "grid",
                groupGridSize = "large",
                usesDynamicColors = true,
                showsBitcoinInSats = true,
            )
        }

        // When
        dataSource.update { it.copy(sharesUsageData = false) }

        // Then
        assertEquals(
            DeviceSettingsEntity(
                id = SINGLE_ROW_ID,
                theme = "dark",
                groupsDisplay = "grid",
                groupGridSize = "large",
                statsChartType = null,
                statsGrain = null,
                usesDynamicColors = true,
                showsBitcoinInSats = true,
                lastUpdatePromptAt = null,
                lastNotificationPromptAt = null,
                isAppBannerDismissed = false,
                sharesUsageData = false,
            ),
            dataSource.observe().first(),
        )
    }

    @Test
    fun `GIVEN usage data sharing off WHEN dark is picked THEN sharing stays off`() = runTest {
        // Given
        prepareScenario()
        dataSource.update { it.copy(sharesUsageData = false) }

        // When
        dataSource.update { it.copy(theme = "dark") }

        // Then
        assertEquals(
            DeviceSettingsEntity(
                id = SINGLE_ROW_ID,
                theme = "dark",
                groupsDisplay = null,
                groupGridSize = null,
                statsChartType = null,
                statsGrain = null,
                usesDynamicColors = false,
                showsBitcoinInSats = false,
                lastUpdatePromptAt = null,
                lastNotificationPromptAt = null,
                isAppBannerDismissed = false,
                sharesUsageData = false,
            ),
            dataSource.observe().first(),
        )
    }

    @Test
    fun `GIVEN usage data sharing off and dark picked WHEN sharing is turned back on THEN stores on keeping dark`() =
        runTest {
            // Given
            prepareScenario()
            dataSource.update {
                it.copy(
                    theme = "dark",
                    sharesUsageData = false,
                )
            }

            // When
            dataSource.update { it.copy(sharesUsageData = true) }

            // Then
            assertEquals(
                DeviceSettingsEntity(
                    id = SINGLE_ROW_ID,
                    theme = "dark",
                    groupsDisplay = null,
                    groupGridSize = null,
                    statsChartType = null,
                    statsGrain = null,
                    usesDynamicColors = false,
                    showsBitcoinInSats = false,
                    lastUpdatePromptAt = null,
                    lastNotificationPromptAt = null,
                    isAppBannerDismissed = false,
                    sharesUsageData = true,
                ),
                dataSource.observe().first(),
            )
        }

    @Test
    fun `GIVEN a device row written without the sharing column WHEN reading it THEN usage data sharing is on`() =
        runTest {
            // Given
            prepareScenario()
            val insertWithoutSharing = "INSERT INTO device_settings (id, theme) VALUES ('$SINGLE_ROW_ID', 'dark')"

            // When
            database.useConnection(isReadOnly = false) { connection -> connection.executeSQL(insertWithoutSharing) }

            // Then
            assertEquals(true, dataSource.observe().first()?.sharesUsageData)
        }

    private fun prepareScenario() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        dataSource = RoomDeviceSettingsLocalDataSource(database.deviceSettingsDao())
    }
}
