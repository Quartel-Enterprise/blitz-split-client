package com.quare.blitzsplit.android.platform

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    // Why: the save sheet may outlive the activity (a rotation, the process reclaimed), so the JSON waits saved.
    var pendingJson by rememberSaveable { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_MIME_TYPE)) { uri ->
        val json = pendingJson
        pendingJson = null
        if (uri != null && json != null) {
            scope.launch { context.writeText(uri, json) }
        }
    }
    return remember(resources, launcher) {
        { json ->
            pendingJson = json
            launcher.launch(resources.getString(R.string.platform_export_file_name, LocalDate.now().toString()))
        }
    }
}

// Why: a provider that vanished or refused the write leaves no file; there is nothing the person can retry here.
private suspend fun Context.writeText(
    uri: Uri,
    text: String,
) {
    withContext(Dispatchers.IO) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
        } catch (failure: IOException) {
            Unit
        } catch (denied: SecurityException) {
            Unit
        }
    }
}
