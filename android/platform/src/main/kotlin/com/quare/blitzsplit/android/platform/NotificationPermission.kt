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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

private const val PREFERENCES_NAME = "notification_permission"
private const val KEY_HAS_ASKED = "has_asked"

/*
 * Why: the app's notification permission, read, asked and sent to the system settings. Android doesn't say
 * whether it ever asked, and its rationale flag is false both before the first ask and after a refusal for good,
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
    // Why: saved, since the activity may be recreated (a rotation) while the system dialog is on top of it.
    val askMemory = rememberSaveable(saver = AskMemory.Saver) { AskMemory() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        /*
         * Why: only an answer moves the rationale (on after a first refusal, off after a refusal for good), while a
         * dialog dismissed by tapping outside it or by Back leaves it as it was and Android may still ask.
         */
        if (isGranted || askMemory.hadRationale != activity.shouldShowNotificationRationale()) {
            activity.markNotificationPermissionAsked()
        }
        onResult(activity.readNotificationPermission())
    }
    return remember(activity, launcher) { NotificationPermissionAccess(activity, launcher, askMemory) }
}

// Why: the rationale flag right before the system dialog, to tell a dismissed dialog from a refusal.
internal class AskMemory(
    var hadRationale: Boolean = false,
) {
    companion object {
        val Saver: Saver<AskMemory, Boolean> = Saver(
            save = { it.hadRationale },
            restore = { AskMemory(hadRationale = it) },
        )
    }
}

private fun Activity.readNotificationPermission(): SystemNotificationPermission = when {
    NotificationManagerCompat.from(this).areNotificationsEnabled() -> SystemNotificationPermission.GRANTED

    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> SystemNotificationPermission.DENIED

    // Why: granted but switched off in the system settings, which no prompt can turn back on.
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED -> SystemNotificationPermission.DENIED

    !hasAskedNotificationPermission() -> SystemNotificationPermission.ASKABLE

    /*
     * Why: a first refusal still lets Android ask again, but the person said no, so the notice shows it like a
     * refusal for good instead of leaving the toggles on and asking at the next one.
     */
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
