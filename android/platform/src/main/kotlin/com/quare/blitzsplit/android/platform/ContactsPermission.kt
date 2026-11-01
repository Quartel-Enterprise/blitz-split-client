package com.quare.blitzsplit.android.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/*
 * Why: the agenda's permission, read, asked and sent to the app's page in the system settings. Android tells a
 * permission never asked from one refused for good by nothing but whether the app asked it before, so the device
 * remembers that it did.
 */
class ContactsPermissionAccess internal constructor(
    private val context: Context,
    private val activity: Activity?,
    private val launcher: ManagedActivityResultLauncher<String, Boolean>,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun isGranted(): Boolean = context.isContactsPermissionGranted()

    val status: ContactsPermissionStatus
        get() {
            val showsRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_CONTACTS)
            } == true
            return when {
                isGranted() -> ContactsPermissionStatus.GRANTED
                showsRationale || preferences.getBoolean(CONTACTS_ASKED, false) -> ContactsPermissionStatus.DENIED
                else -> ContactsPermissionStatus.NOT_ASKED
            }
        }

    fun request() {
        preferences.edit { putBoolean(CONTACTS_ASKED, true) }
        launcher.launch(Manifest.permission.READ_CONTACTS)
    }

    fun openSettings() {
        context.openAppSettings()
    }

    private companion object {
        const val PREFERENCES = "platform_permissions"
        const val CONTACTS_ASKED = "contacts_asked"
    }
}

// Why: Contatos' search reads the agenda only with the access it already has; it never asks.
fun Context.isContactsPermissionGranted(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

@Composable
fun rememberContactsPermissionAccess(onResult: (isGranted: Boolean) -> Unit): ContactsPermissionAccess {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    return remember(context, activity, launcher) { ContactsPermissionAccess(context, activity, launcher) }
}
