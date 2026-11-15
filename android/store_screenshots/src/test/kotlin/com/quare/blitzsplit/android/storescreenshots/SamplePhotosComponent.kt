package com.quare.blitzsplit.android.storescreenshots

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import java.io.File

private const val FACES_DIRECTORY_PROPERTY = "storeScreenshots.facesDir"
private const val GROUP_PHOTOS_DIRECTORY_PROPERTY = "storeScreenshots.groupPhotosDir"

/**
 * Hands every avatar its sample face, and every group its sample photo, at once: Coil's preview handler answers
 * synchronously, where a real load would still be on its way when Robolectric captures the frame. The handler only
 * runs in inspection mode.
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
internal fun SamplePhotosComponent(content: @Composable () -> Unit) {
    val directory =
        checkNotNull(System.getProperty(FACES_DIRECTORY_PROPERTY)) { "$FACES_DIRECTORY_PROPERTY is not set" }
    val groupPhotos = checkNotNull(System.getProperty(GROUP_PHOTOS_DIRECTORY_PROPERTY)) {
        "$GROUP_PHOTOS_DIRECTORY_PROPERTY is not set"
    }
    val handler = AsyncImagePreviewHandler { request ->
        val data = request.data as? String
        val file = when {
            data == null -> null
            data.startsWith(FACE_SCHEME) -> File(directory, "person${data.removePrefix(FACE_SCHEME)}.jpg")
            data.startsWith(GROUP_PHOTO_SCHEME) -> File(groupPhotos, "${data.removePrefix(GROUP_PHOTO_SCHEME)}.jpg")
            else -> null
        }
        file?.takeIf(File::isFile)?.let { BitmapFactory.decodeFile(it.path).asImage() } ?: ColorImage()
    }
    CompositionLocalProvider(
        LocalInspectionMode provides true,
        LocalAsyncImagePreviewHandler provides handler,
        content = content,
    )
}
