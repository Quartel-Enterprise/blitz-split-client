package com.quare.blitzsplit.core.model

import kotlin.io.encoding.Base64

/*
 * Why: the photo the editor frames, already upright as a JPEG of at most 1600 px (what `BillImageEncoder` makes of a
 * pick), so the three UIs draw it without reading its EXIF, and its size in pixels for the editor's geometry.
 */
class CropPhoto(
    val jpeg: ByteArray,
    val width: Int,
    val height: Int,
) {
    // Why: the web draws it in an <img>, and each state copy while dragging shares this instance.
    val dataUri: String by lazy { "data:image/jpeg;base64,${Base64.encode(jpeg)}" }
}
