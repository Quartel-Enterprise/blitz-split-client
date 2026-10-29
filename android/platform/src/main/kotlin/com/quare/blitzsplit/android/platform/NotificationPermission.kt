package com.quare.blitzsplit.android.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

private const val PREFERENCES_NAME = "notification_permission"
private const val KEY_HAS_ASKED = "has_asked"

/*
 * Why: the app's notification permission, read, asked and sent to the system settings. Android tells a refusal
 * for good from one it may ask again only through the rationale flag, which is also false before the first ask,
 * so the app remembers that it asked.
 */
class NotificationPermissionAccess internal constructor(
    private val activity: Activity,
    private val launcher: ManagedActivityResultLauncher<String, Boolean>,
    private val askMemory: AskMemory,
) {
    fun read(): SystemNotificationPermission = activity.readNotificationPermission()

    fun request() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askMemory.hadRationale = activity.shouldShowNotificationRationale()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun openSettings() {
        activity.startActivity(
            Intent(
                Settings.ACTION_APP_NOTIFICATION_SETTINGS,
            ).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName),
        )
    }
}

// Why: onResult gets the permission as read right after the person answers the system dialog.
@Composable
fun rememberNotificationPermissionAccess(
    onResult: (SystemNotificationPermission) -> Unit,
): NotificationPermissionAccess {
    val activity = requireNotNull(LocalActivity.current)
    val askMemory = remember { AskMemory() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        /*
         * Why: a dialog dismissed by tapping outside it leaves the rationale as it was and Android may still ask, so
         * only an answer that moved or kept the rationale on counts as asked.
         */
        if (isGranted || askMemory.hadRationale || activity.shouldShowNotificationRationale()) {
            activity.markNotificationPermissionAsked()
        }
        onResult(activity.readNotificationPermission())
    }
    return remember(activity, launcher) { NotificationPermissionAccess(activity, launcher, askMemory) }
}

// Why: the rationale flag right before the system dialog, to tell a dismissed dialog from a refusal.
internal class AskMemory {
    var hadRationale: Boolean = false
}

private fun Activity.readNotificationPermission(): SystemNotificationPermission = when {
    NotificationManagerCompat.from(this).areNotificationsEnabled() -> SystemNotificationPermission.GRANTED

    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> SystemNotificationPermission.DENIED

    // Why: granted but switched off in the system settings, which no prompt can turn back on.
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED -> SystemNotificationPermission.DENIED

    !hasAskedNotificationPermission() -> SystemNotificationPermission.ASKABLE

    shouldShowNotificationRationale() -> SystemNotificationPermission.ASKABLE

    else -> SystemNotificationPermission.DENIED
}

private fun Activity.shouldShowNotificationRationale(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)

private fun Context.hasAskedNotificationPermission(): Boolean =
    getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(KEY_HAS_ASKED, false)

private fun Context.markNotificationPermissionAsked() {
    getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit { putBoolean(KEY_HAS_ASKED, true) }
}
