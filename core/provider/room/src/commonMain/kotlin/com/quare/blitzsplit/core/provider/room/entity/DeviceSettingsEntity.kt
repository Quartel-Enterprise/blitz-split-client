package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: what belongs to this device and not to the person, so it is never synced and outlives a
 * sign out.
 */
@Entity(tableName = "device_settings")
data class DeviceSettingsEntity(
    @PrimaryKey
    val id: String,
    val theme: String,
    // Why: the home's "Lista" or "Grade"; null until picked on this device.
    @ColumnInfo(defaultValue = "NULL")
    val groupsDisplay: String?,
    // Why: the grid's "Compacto", "Padrão" or "Grande"; null until picked on this device.
    @ColumnInfo(defaultValue = "NULL")
    val groupGridSize: String?,
    // Why: "Barras" or "Linha" in a group's statistics; null until picked on this device.
    @ColumnInfo(defaultValue = "NULL")
    val statsChartType: String?,
    // Why: "Dia", "Semana", "Mês" or "Ano" in a group's statistics; null until picked on this device.
    @ColumnInfo(defaultValue = "NULL")
    val statsGrain: String?,
    // Why: Material You (Android 12 and later), the scheme drawn from the wallpaper instead of the brand's; off until turned on.
    @ColumnInfo(defaultValue = "0")
    val usesDynamicColors: Boolean,
    // Why: "Bitcoin em satoshis", every bitcoin amount shown and typed in whole sats; off until turned on.
    @ColumnInfo(defaultValue = "0")
    val showsBitcoinInSats: Boolean,
    /*
     * Why: when this device last offered an update (epoch millis), so opening the app again within the hour doesn't
     * ask again; null until offered once.
     */
    @ColumnInfo(defaultValue = "NULL")
    val lastUpdatePromptAt: Long?,
    /*
     * Why: when Home's notification card was last answered ("Turn on" or "Not now", epoch millis), so it comes back
     * only weeks later; null until answered once.
     */
    @ColumnInfo(defaultValue = "NULL")
    val lastNotificationPromptAt: Long?,
)
