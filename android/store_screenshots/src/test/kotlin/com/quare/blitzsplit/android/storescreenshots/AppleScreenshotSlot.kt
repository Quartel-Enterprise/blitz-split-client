package com.quare.blitzsplit.android.storescreenshots

/**
 * The App Store sizes the UI tests capture (iosApp/UITests, scripts/store-screenshots/capture-ios.sh), each on a
 * device whose screen matches what the frame draws it into:
 *
 * - [IPHONE_6_3]: Dynamic Island (medium display), the size App Store Connect requires: an iPhone 17 Pro, 402x874.
 * - [IPHONE_6_7]: Dynamic Island (large display), 430x932: an iPhone 16 Plus, the frame's own AppleIPhone67 screen.
 * - [IPAD_13]: the 13" iPad lying down, 1366x1024 points (2732x2048 px): an iPad Air 13", in a frame of its own, since
 *   the library's iPad frame only stands upright. Upright, the sidebar the iPad opens with covers the screen. The 11"
 *   frame draws the same capture.
 * - [IPHONE_DUO]: the iPhone Duo closed, as the simulator boots it: its outer display at 1398x2034 px, in a frame of its
 *   own.
 * - [IPHONE_DUO_OPEN]: the iPhone Duo open and lying down, once Device Hub opened it: its inner display, 2853x2007 px,
 *   which XCTest hands a pixel short each way (2852x2006) and the frame draws on the display's own canvas.
 * - [MAC]: the Mac app's window at 1440x900 points, in a window frame on a 2880x1800 canvas.
 *
 * [isWide] says which way the slot's captures lie, wider than tall or not, so the frame refuses one that lies the
 * other way instead of drawing it stretched: an iPad capture from before the slot was captured lying down.
 */
internal enum class AppleScreenshotSlot(
    val directoryName: String,
    val isWide: Boolean,
) {
    IPHONE_6_3(
        directoryName = "iphone63",
        isWide = false,
    ),
    IPHONE_6_7(
        directoryName = "iphone67",
        isWide = false,
    ),
    IPAD_13(
        directoryName = "ipad13",
        isWide = true,
    ),
    IPHONE_DUO(
        directoryName = "iphoneduo",
        isWide = false,
    ),
    IPHONE_DUO_OPEN(
        directoryName = "iphoneduoopen",
        isWide = true,
    ),
    MAC(
        directoryName = "mac",
        isWide = true,
    ),
}
