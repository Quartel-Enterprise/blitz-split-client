package com.quare.blitzsplit.core.model

import kotlin.math.max
import kotlin.math.min

/*
 * Why: where the photo editor puts the photo in its frame (the circle of a profile, the rounded square of a group), the
 * same on the three UIs. Lengths are fractions of the frame's side, so each UI draws it at its own size and a window
 * resized keeps the framing. The photo is drawn mirrored first (horizontally), then turned clockwise, then zoomed, its
 * center moved by the offset from the frame's center. It covers the whole frame, unless `canFit` lets it zoom out until
 * it fits whole (a bill's receipt), the rest of the frame white as the encoders fill it.
 */
data class PhotoCrop(
    val photoWidth: Int,
    val photoHeight: Int,
    val quarterTurns: Int,
    val isMirrored: Boolean,
    val zoom: Float,
    val offsetX: Float,
    val offsetY: Float,
    val canFit: Boolean,
) {
    // Why: at zoom 1 the photo's shorter side fills the frame, whichever way it is turned.
    private val coverScale: Float
        get() = 1f / min(photoWidth, photoHeight)

    // Why: the UIs size the upright photo with these, then mirror, turn, zoom and move it as above.
    val photoWidthInFrame: Float
        get() = photoWidth * coverScale

    val photoHeightInFrame: Float
        get() = photoHeight * coverScale

    val rotationDegrees: Int
        get() = quarterTurns * QUARTER_TURN_DEGREES

    // Why: a receipt longer than the frame zooms out until its longer side fits (feedback of 2026-11-25).
    val minZoom: Float
        get() = if (canFit) min(photoWidth, photoHeight).toFloat() / max(photoWidth, photoHeight) else COVER_ZOOM

    val maxZoom: Float
        get() = MAX_ZOOM

    private val isSideways: Boolean
        get() = quarterTurns % 2 == 1

    fun toDragged(
        dx: Float,
        dy: Float,
    ): PhotoCrop = copy(
        offsetX = offsetX + dx,
        offsetY = offsetY + dy,
    ).clamp()

    // Why: the point of the photo under the focus (from the frame's center) stays there, as under a pinch's fingers.
    fun toZoomedBy(
        factor: Float,
        focusX: Float = 0f,
        focusY: Float = 0f,
    ): PhotoCrop {
        val newZoom = (zoom * factor).coerceIn(minZoom, MAX_ZOOM)
        val scale = newZoom / zoom
        return copy(
            zoom = newZoom,
            offsetX = focusX + (offsetX - focusX) * scale,
            offsetY = focusY + (offsetY - focusY) * scale,
        ).clamp()
    }

    fun toZoomed(zoom: Float): PhotoCrop = copy(zoom = zoom.coerceIn(minZoom, MAX_ZOOM)).clamp()

    // Why: the frame's center stays on the same point of the photo, which turns around it.
    fun toTurned(): PhotoCrop = copy(
        quarterTurns = (quarterTurns + 1) % QUARTER_TURNS,
        offsetX = -offsetY,
        offsetY = offsetX,
    ).clamp()

    // Why: mirrored on screen, after the turn, which reads as the opposite turn of a photo mirrored before it.
    fun toMirroredHorizontally(): PhotoCrop = copy(
        quarterTurns = (QUARTER_TURNS - quarterTurns) % QUARTER_TURNS,
        isMirrored = !isMirrored,
        offsetX = -offsetX,
    )

    // Why: mirroring upside down is mirroring sideways and a half turn.
    fun toMirroredVertically(): PhotoCrop = copy(
        quarterTurns = (QUARTER_TURNS + 2 - quarterTurns) % QUARTER_TURNS,
        isMirrored = !isMirrored,
        offsetY = -offsetY,
    )

    // Why: the photo never moves so far that the frame shows past its edge; a side shorter than the frame stays centered.
    private fun clamp(): PhotoCrop {
        val width = (if (isSideways) photoHeight else photoWidth) * coverScale * zoom
        val height = (if (isSideways) photoWidth else photoHeight) * coverScale * zoom
        val maxX = max(0f, (width - 1f) / HALF)
        val maxY = max(0f, (height - 1f) / HALF)
        return copy(
            offsetX = offsetX.coerceIn(-maxX, maxX),
            offsetY = offsetY.coerceIn(-maxY, maxY),
        )
    }

    companion object {
        private const val COVER_ZOOM = 1f
        private const val MAX_ZOOM = 3f
        private const val QUARTER_TURNS = 4
        private const val QUARTER_TURN_DEGREES = 90
        private const val HALF = 2f

        // Why: the photo as picked, its center in the frame's, covering it.
        fun of(
            photoWidth: Int,
            photoHeight: Int,
            canFit: Boolean = false,
        ): PhotoCrop = PhotoCrop(
            photoWidth = photoWidth,
            photoHeight = photoHeight,
            quarterTurns = 0,
            isMirrored = false,
            zoom = COVER_ZOOM,
            offsetX = 0f,
            offsetY = 0f,
            canFit = canFit,
        )
    }
}
