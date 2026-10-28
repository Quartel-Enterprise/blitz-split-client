package com.quare.blitzsplit.android.platform

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/*
 * Why: the shared code decodes and shrinks the photo, so the UI only hands it the file's bytes; a file the
 * picker or the camera app can't give back is null, and a cancelled read still cancels.
 */
suspend fun Context.readPhotoBytes(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    try {
        contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (exception: IOException) {
        null
    } catch (exception: SecurityException) {
        null
    }
}

// Why: the camera app writes the full size photo here, as a thumbnail would turn blurry at the avatar's 512px.
fun Context.createCameraPhotoUri(): Uri {
    val file = File(cacheDir, "camera").apply { mkdirs() }.resolve("photo.jpg")
    return FileProvider.getUriForFile(this, "$packageName.photos", file)
}
