package com.quare.blitzsplit.android.platform

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val DATA_URI_PREFIX = "data:"
private const val JPEG_MIME_TYPE = "image/jpeg"
private const val CONNECT_TIMEOUT_MILLIS = 15_000
private const val READ_TIMEOUT_MILLIS = 30_000

// Why: one photo to keep, the image the screen draws (a data URI or an https URL) and the file's name in the gallery.
private class PendingPhoto(
    val imageUrl: String,
    val fileName: String,
)

/*
 * Why: a bill's "Baixar" keeps the photo in the gallery, in Pictures/Bora Rachar (Blitz Split in English). Android 10
 * and later take it through MediaStore with no permission; older ones need the storage permission, asked for only
 * then (the manifest stops declaring it from Android 10). A refusal saves nothing and says nothing, as a cancel.
 */
@Composable
fun rememberGalleryPhotoSaver(onResult: (isSaved: Boolean) -> Unit): (imageUrl: String, fileName: String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)
    var pending by remember { mutableStateOf<PendingPhoto?>(null) }
    val save = { photo: PendingPhoto ->
        scope.launch { currentOnResult(context.savePhotoToGallery(photo)) }
        Unit
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        val photo = pending
        pending = null
        if (isGranted && photo != null) save(photo)
    }
    return remember(context, permission) {
        { imageUrl, fileName ->
            val photo = PendingPhoto(imageUrl, fileName)
            if (context.canWriteToGallery()) {
                save(photo)
            } else {
                pending = photo
                permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
}

private fun Context.canWriteToGallery(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
    PackageManager.PERMISSION_GRANTED

private suspend fun Context.savePhotoToGallery(photo: PendingPhoto): Boolean = withContext(Dispatchers.IO) {
    val bytes = readImageBytes(photo.imageUrl) ?: return@withContext false
    val folder = getString(R.string.platform_app_name)
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            insertIntoMediaStore(bytes, photo.fileName, folder)
        } else {
            writeToPicturesFolder(bytes, photo.fileName, folder)
        }
    } catch (failure: IOException) {
        false
    } catch (denied: SecurityException) {
        false
    }
}

// Why: a photo added on this device is still a data URI until it is up; any other is the server's signed URL.
private fun readImageBytes(imageUrl: String): ByteArray? = if (imageUrl.startsWith(DATA_URI_PREFIX)) {
    try {
        Base64.decode(imageUrl.substringAfter(','), Base64.DEFAULT)
    } catch (malformed: IllegalArgumentException) {
        null
    }
} else {
    try {
        val connection = URL(imageUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = READ_TIMEOUT_MILLIS
        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { it.readBytes() }
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    } catch (failure: IOException) {
        null
    }
}

// Why: pending until written, so the gallery never shows a half written photo; a failed write leaves no row behind.
@RequiresApi(Build.VERSION_CODES.Q)
private fun Context.insertIntoMediaStore(
    bytes: ByteArray,
    fileName: String,
    folder: String,
): Boolean {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, JPEG_MIME_TYPE)
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$folder")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val uri = contentResolver.insert(collection, values) ?: return false
    return try {
        val isWritten = contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
        if (isWritten) {
            contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null,
            )
        } else {
            contentResolver.delete(uri, null, null)
        }
        isWritten
    } catch (failure: IOException) {
        contentResolver.delete(uri, null, null)
        false
    }
}

// Why: before Android 10 the gallery reads the shared Pictures folder once the media scanner has seen the file.
@Suppress("DEPRECATION")
private fun Context.writeToPicturesFolder(
    bytes: ByteArray,
    fileName: String,
    folder: String,
): Boolean {
    val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folder)
    if (!directory.isDirectory && !directory.mkdirs()) return false
    val file = uniqueFileOf(directory, fileName)
    file.writeBytes(bytes)
    MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), arrayOf(JPEG_MIME_TYPE), null)
    return true
}

// Why: "Baixar" twice keeps both, "foto-2 (1).jpg" next to "foto-2.jpg", as MediaStore names them from Android 10.
private fun uniqueFileOf(
    directory: File,
    fileName: String,
): File {
    val base = fileName.substringBeforeLast('.')
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
    var file = File(directory, fileName)
    var copy = 1
    while (file.exists()) {
        file = File(directory, if (extension.isEmpty()) "$base ($copy)" else "$base ($copy).$extension")
        copy++
    }
    return file
}
