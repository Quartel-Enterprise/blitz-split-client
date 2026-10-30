package com.quare.blitzsplit.android.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// Why: the camera permission of the invite reader and of "Tirar foto", read, asked and sent to the app's page in the system settings.
class CameraPermissionAccess internal constructor(
    private val context: Context,
    private val activity: Activity?,
    private val launcher: ManagedActivityResultLauncher<String, Boolean>,
) {
    fun isGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    /*
     * Why: once refused for good, Android answers a request with a refusal and shows nothing, so only the settings can
     * allow the camera. Read right after a refusal: Android stops offering the rationale only for a refusal for good.
     */
    fun isDeniedForGood(): Boolean = !isGranted() &&
        activity?.let { !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } == true

    fun request() {
        launcher.launch(Manifest.permission.CAMERA)
    }

    fun openSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
fun rememberCameraPermissionAccess(
    onResult: CameraPermissionAccess.(isGranted: Boolean) -> Unit,
): CameraPermissionAccess {
    val context = LocalContext.current
    val activity = LocalActivity.current
    // Why: the result may need the access itself (is it refused for good?), which only exists once the launcher does.
    lateinit var access: CameraPermissionAccess
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        access.onResult(isGranted)
    }
    access = remember(context, activity, launcher) { CameraPermissionAccess(context, activity, launcher) }
    return access
}
