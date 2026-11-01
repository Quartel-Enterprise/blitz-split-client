package com.quare.blitzsplit.feature.splitbill.presentation.model

/*
 * Why: a person of "Item" as the UIs draw them: what they typed, their part of the item (null while it doesn't add
 * up or they are out of it) and, while it doesn't close, whether they are the one it misses next to (7.6f).
 */
data class ItemPersonLine(
    val row: ItemPersonRow,
    val shareMinor: Long?,
    val isError: Boolean,
)
