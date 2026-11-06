package com.quare.blitzsplit.core.model

/*
 * Why: where "Scan receipt" takes the receipt's photo from, picked on its choice (beta feedback, part 2, item 7), on
 * Split a bill and on Home's shortcut alike (beta feedback, part 3, item 16). Each platform offers the ones it has: the
 * camera, the gallery and a file on Android and iOS, Photos and a file on the Mac, the camera and a file on the web.
 */
enum class ReceiptPhotoSource {
    // Why: the app's own camera, with the receipt's frame (the browser's on the web).
    CAMERA,

    // Why: the system photo picker: Android's Photo Picker, Apple's PhotosPicker.
    PHOTOS,

    // Why: a file: Android's and iOS's documents picker, the Mac's file panel, the browser's file chooser.
    FILES,
}
