package com.quare.blitzsplit.android.feature.notificationsettings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationPermission

private const val PREFERENCES_NAME = "notification_permission"
private const val KEY_HAS_ASKED = "has_asked"

/*
 * Why: Android tells "never asked" from "denied" only by a flag the app keeps, since both read as not granted.
 * Before Android 13 there is nothing to ask, only the switch in the system settings.
 */
internal fun Context.readNotificationPermission(): NotificationPermission = when {
    NotificationManagerCompat.from(this).areNotificationsEnabled() -> NotificationPermission.GRANTED

    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> NotificationPermission.DENIED

    hasAskedNotificationPermission() -> NotificationPermission.DENIED

    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED -> NotificationPermission.DENIED

    else -> NotificationPermission.NOT_DETERMINED
}

internal fun Context.markNotificationPermissionAsked() {
    getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit { putBoolean(KEY_HAS_ASKED, true) }
}

internal fun Context.openNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

private fun Context.hasAskedNotificationPermission(): Boolean =
    getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(KEY_HAS_ASKED, false)
