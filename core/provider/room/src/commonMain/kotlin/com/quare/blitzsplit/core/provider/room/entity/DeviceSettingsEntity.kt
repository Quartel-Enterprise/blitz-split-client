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
)
