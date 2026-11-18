package com.quare.blitzsplit.android.storescreenshots

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

private const val CAPTURES_DIRECTORY_PROPERTY = "appleScreenshots.capturesDir"

/** The screen the iOS UI tests captured on the simulator, which the App Store classes frame in place of a Compose one. */
@Composable
internal fun AppleScreenshotImage(
    slot: AppleScreenshotSlot,
    locale: String,
    fileName: String,
) {
    val capture = remember(slot, locale, fileName) {
        val directory = checkNotNull(System.getProperty(CAPTURES_DIRECTORY_PROPERTY)) {
            "$CAPTURES_DIRECTORY_PROPERTY is not set: generate the App Store screenshots through an *AppStoreScreenshots task"
        }
        val file = File(directory, "${slot.directoryName}/$locale/$fileName.png")
        check(file.isFile) { "No iOS capture at $file: run scripts/store-screenshots/capture-ios.sh first" }
        val bitmap = checkNotNull(BitmapFactory.decodeFile(file.path)) {
            "The capture at $file is no PNG the frame can read: capture it again"
        }
        check((bitmap.width > bitmap.height) == slot.isWide) {
            "The capture at $file is ${bitmap.width}x${bitmap.height}, which the frame of ${slot.directoryName} " +
                "would draw stretched: capture the slot again with scripts/store-screenshots/capture-ios.sh"
        }
        bitmap.asImageBitmap()
    }
    Image(
        bitmap = capture,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds,
        filterQuality = FilterQuality.High,
    )
}

/**
 * Whether the slot was captured in every language of the listings: captures older than a language (a Mac capture kept
 * by a run that left the Mac out) count as none, so their slot is skipped rather than failing on the missing folder.
 */
internal fun hasAppleCaptures(slot: AppleScreenshotSlot): Boolean = System
    .getProperty(CAPTURES_DIRECTORY_PROPERTY)
    ?.let { captures -> storeLocales.all { File(captures, "${slot.directoryName}/${it.tag}").isDirectory } } == true
