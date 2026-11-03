package com.quare.blitzsplit.push

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.quare.blitzsplit.android.platform.SystemNotificationPermission
import com.quare.blitzsplit.android.platform.rememberNotificationPermissionAccess
import com.quare.blitzsplit.core.navigation.Route

/*
 * Why: Android 13 and later show no notification until the person allows them, so Início asks once, the first time
 * it shows, while push is set up. A refusal is final here: the toggles of Perfil › Notificações lead to the settings.
 */
@Composable
fun NotificationPermissionEffect(top: Route?) {
    if (!PushSetup.isConfigured) return
    val access = rememberNotificationPermissionAccess(onResult = {})
    var hasAsked by rememberSaveable { mutableStateOf(false) }
    val isOnHome = top == Route.Home
    LaunchedEffect(isOnHome) {
        if (isOnHome && !hasAsked && access.read() == SystemNotificationPermission.ASKABLE) {
            hasAsked = true
            access.request()
        }
    }
}
