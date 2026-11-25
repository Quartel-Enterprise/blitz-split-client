package com.quare.blitzsplit.core.model

// Why: the image in a tile whose side is 1: its size and its top left corner, which may sit outside the tile.
data class PhotoFramePlacement(
    val width: Float,
    val height: Float,
    val x: Float,
    val y: Float,
)
