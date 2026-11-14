package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity

/*
 * Why: the last text of a legal page (LegalPage.slug) the site answered for a language, as its Markdown, so the page
 * opens offline and at once. language is the one asked for ("fr"), which the site may have answered in English. Kept
 * as it came and read again each time it is drawn, so an app that learns more of the Markdown shows it without a
 * download.
 */
@Entity(
    tableName = "legal_document",
    primaryKeys = ["page", "language"],
)
data class LegalDocumentEntity(
    val page: String,
    val language: String,
    val markdown: String,
    // Why: the site's ETag for this text, sent back so an unchanged one answers 304 with no body; null without one.
    @ColumnInfo(defaultValue = "NULL")
    val etag: String?,
    // Why: when the site last confirmed this text (epoch millis); null for a text saved before the app kept it.
    @ColumnInfo(defaultValue = "NULL")
    val checkedAt: Long?,
)
