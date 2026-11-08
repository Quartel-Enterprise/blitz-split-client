package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: the color this device's person gave one of the app's own categories (7.7), by its key ("food"). The app's
 * categories are the same for everyone, so unlike a group's they are not shared with the group: only the rows the
 * person changed are kept, the rest read as the app's default.
 */
@Entity(tableName = "app_category_color")
data class AppCategoryColorEntity(
    @PrimaryKey
    val categoryKey: String,
    // Why: a CategoryColor code.
    val color: String,
)
