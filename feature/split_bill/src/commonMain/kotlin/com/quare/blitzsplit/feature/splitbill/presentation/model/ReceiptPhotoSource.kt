package com.quare.blitzsplit.feature.splitbill.presentation.model

/*
 * Why: where "Scan receipt" takes the receipt's photo from, picked on its choice (beta feedback, part 2, item 7). Each
 * platform offers the ones it has: the camera and the gallery on Android and iOS, Photos and a file on the Mac, the
 * camera and a file on the web.
 */
enum class ReceiptPhotoSource {
    // Why: the app's own camera, with the receipt's frame (the browser's on the web).
    CAMERA,

    // Why: the system photo picker: Android's Photo Picker, Apple's PhotosPicker.
    PHOTOS,

    // Why: a file: the Mac's file panel, the browser's file chooser.
    FILES,
}
