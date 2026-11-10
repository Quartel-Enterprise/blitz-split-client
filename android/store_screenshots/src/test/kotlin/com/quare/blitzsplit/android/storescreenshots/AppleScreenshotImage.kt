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
        BitmapFactory.decodeFile(file.path).asImageBitmap()
    }
    Image(
        bitmap = capture,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds,
        filterQuality = FilterQuality.High,
    )
}

internal fun hasAppleCaptures(slot: AppleScreenshotSlot): Boolean = System
    .getProperty(CAPTURES_DIRECTORY_PROPERTY)
    ?.let { File(it, slot.directoryName).isDirectory } == true
