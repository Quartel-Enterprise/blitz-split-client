package com.quare.blitzsplit.shared.storescreenshots

import com.quare.blitzsplit.shared.bridge.BlitzSplitApple
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile

// Why: the Apple captures hand the photos in the launch environment, which the app writes to a folder of its own.
internal class DirectoryAssets(
    private val directory: String,
) : StoreScreenshotsAssets {
    override fun read(path: String): ByteArray? =
        NSData.dataWithContentsOfFile("$directory/$path")?.let(BlitzSplitApple::toByteArray)
}
