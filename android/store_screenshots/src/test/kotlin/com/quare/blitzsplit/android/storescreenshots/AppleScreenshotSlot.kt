package com.quare.blitzsplit.android.storescreenshots

/**
 * The App Store sizes the UI tests capture (iosApp/UITests, scripts/store-screenshots/capture-ios.sh), each on a
 * device whose screen matches what the frame draws it into:
 *
 * - [IPHONE_6_3]: Dynamic Island (medium display), the size App Store Connect requires: an iPhone 17 Pro, 402x874.
 * - [IPHONE_6_7]: Dynamic Island (large display), 430x932: an iPhone 16 Plus, the frame's own AppleIPhone67 screen.
 * - [IPAD_13]: the 13" iPad, 1024x1366: an iPad Air 13", the frame's AppleIPad13 screen.
 * - [IPHONE_DUO]: the iPhone Duo closed, as the simulator boots it: its outer display at 1398x2034 px, in a frame of its
 *   own.
 * - [MAC]: the Mac app's window at 1440x900 points, in a window frame on a 2880x1800 canvas.
 */
internal enum class AppleScreenshotSlot(
    val directoryName: String,
) {
    IPHONE_6_3(directoryName = "iphone63"),
    IPHONE_6_7(directoryName = "iphone67"),
    IPAD_13(directoryName = "ipad13"),
    IPHONE_DUO(directoryName = "iphoneduo"),
    MAC(directoryName = "mac"),
}
