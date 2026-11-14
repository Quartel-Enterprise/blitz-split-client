package com.quare.blitzsplit.android.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate

private const val EXPORT_MIME_TYPE = "application/json"

/*
 * Why: "Exportar meus dados" hands the JSON to the system's save sheet (Storage Access Framework), so the
 * person picks where it goes (Downloads, Drive) and the app needs no storage permission.
 */
@Composable
fun rememberExportFileSaver(): (json: String) -> Unit {
    val resources = LocalResources.current
    val saveFile = rememberFileSaver()
    return remember(resources, saveFile) {
        { json ->
            saveFile(
                resources.getString(R.string.platform_export_file_name, LocalDate.now().toString()),
                EXPORT_MIME_TYPE,
                json.encodeToByteArray(),
            )
        }
    }
}

/*
 * Why: a file the app made (an export) handed to the system's save sheet under its name and type, so the person picks
 * where it goes and the app needs no storage permission.
 */
@Composable
fun rememberFileSaver(): (fileName: String, mimeType: String, bytes: ByteArray) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Why: in memory, not the saved state, which the system may write to disk with what the file holds.
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    val launcher = rememberLauncherForActivityResult(CreateTypedDocument()) { uri ->
        val bytes = pendingBytes
        pendingBytes = null
        when {
            uri == null -> Unit

            bytes != null -> scope.launch { context.writeBytes(uri, bytes) }

            // Why: the activity was recreated while the sheet was open, so the file is gone; no empty file stays.
            else -> scope.launch { context.deleteDocument(uri) }
        }
    }
    return remember(launcher) {
        { fileName, mimeType, bytes ->
            pendingBytes = bytes
            launcher.launch(TypedDocument(fileName, mimeType))
        }
    }
}

private class TypedDocument(
    val fileName: String,
    val mimeType: String,
)

// Why: CreateDocument fixes its type when built, and each export format has its own.
private class CreateTypedDocument : ActivityResultContract<TypedDocument, Uri?>() {
    override fun createIntent(
        context: Context,
        input: TypedDocument,
    ): Intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType(input.mimeType)
        .putExtra(Intent.EXTRA_TITLE, input.fileName)

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): Uri? = intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}

private suspend fun Context.deleteDocument(uri: Uri) {
    withContext(Dispatchers.IO) {
        try {
            DocumentsContract.deleteDocument(contentResolver, uri)
        } catch (failure: IOException) {
            Unit
        } catch (denied: SecurityException) {
            Unit
        } catch (unsupported: UnsupportedOperationException) {
            Unit
        }
    }
}

// Why: a provider that vanished or refused the write leaves no file; there is nothing the person can retry here.
private suspend fun Context.writeBytes(
    uri: Uri,
    bytes: ByteArray,
) {
    withContext(Dispatchers.IO) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
        } catch (failure: IOException) {
            Unit
        } catch (denied: SecurityException) {
            Unit
        }
    }
}
