package com.quare.blitzsplit.android.storescreenshots

/**
 * The App Store sizes the iOS UI tests capture, and the simulator each one runs on (iosApp/UITests, see
 * scripts/store-screenshots/capture-ios.sh). The capture's points must match the store-screenshots frame's screen
 * (FormFactor.AppleIPhone65 is 428x926, AppleIPhone67 430x932, AppleIPad13 1024x1366), or the PNG is stretched.
 */
internal enum class AppleScreenshotSlot(
    val directoryName: String,
) {
    IPHONE_6_5(directoryName = "iphone65"),
    IPHONE_6_7(directoryName = "iphone67"),
    IPAD_13(directoryName = "ipad13"),
}
