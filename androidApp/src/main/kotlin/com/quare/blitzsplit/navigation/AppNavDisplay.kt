package com.quare.blitzsplit.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.quare.blitzsplit.android.designsystem.layout.rememberUsesFormDialog
import com.quare.blitzsplit.android.feature.creategroup.CreateGroupScreen
import com.quare.blitzsplit.android.feature.deleteaccount.DeleteAccountScreen
import com.quare.blitzsplit.android.feature.editprofile.EditProfileScreen
import com.quare.blitzsplit.android.feature.groups.GroupsScreen
import com.quare.blitzsplit.android.feature.notificationsettings.NotificationSettingsScreen
import com.quare.blitzsplit.android.feature.paymentkey.PaymentKeyScreen
import com.quare.blitzsplit.android.feature.profile.ProfileScreen
import com.quare.blitzsplit.android.feature.profilesetup.ProfileSetupScreen
import com.quare.blitzsplit.android.feature.recoverycode.RecoveryCodeSheet
import com.quare.blitzsplit.android.feature.restore.RestoreScreen
import com.quare.blitzsplit.android.feature.welcome.WelcomeScreen
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.shared.app.AppUiState
import com.quare.blitzsplit.shared.app.AppViewModel
import org.koin.compose.viewmodel.koinViewModel

/*
 * Why: the back stack lives in :core:navigation and the ViewModels move it; Navigation 3 only
 * draws its entries, so Android walks the same flow as SwiftUI and React.
 */
@Composable
fun AppNavDisplay(appViewModel: AppViewModel = koinViewModel()) {
    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
    val backStack by appViewModel.navigator.backStack.collectAsStateWithLifecycle()
    if (uiState == AppUiState.LOADING) {
        // Why: the start route depends on whether a profile exists, read from Room in a moment.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {}
        return
    }
    val usesFormDialog = rememberUsesFormDialog()
    val formDialogMetadata = if (usesFormDialog) {
        DialogSceneStrategy.dialog(
            DialogProperties(
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
        )
    } else {
        emptyMap()
    }
    NavDisplay(
        backStack = backStack,
        onBack = { appViewModel.navigator.navigateBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(rememberSaveableStateHolder()),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        sceneStrategies = listOf(DialogSceneStrategy()),
        entryProvider = entryProvider {
            entry<Route.Welcome> { WelcomeScreen() }
            entry<Route.ProfileSetup> { ProfileSetupScreen() }
            entry<Route.Restore> { RestoreScreen() }
            entry<Route.Groups> { GroupsScreen(recoveryCodeSheet = { RecoveryCodeSheet() }) }
            entry<Route.Profile> { ProfileScreen(recoveryCodeSheet = { RecoveryCodeSheet() }) }
            entry<Route.EditProfile> { EditProfileScreen() }
            entry<Route.NotificationSettings> { NotificationSettingsScreen() }
            // Why: a tablet keeps the profile behind the key, a phone or an unfolded foldable opens it full screen.
            entry<Route.PaymentKey>(metadata = formDialogMetadata) { route ->
                PaymentKeyScreen(
                    route = route,
                    isDialog = usesFormDialog,
                )
            }
            // Why: like a payment key, a tablet keeps the profile behind it; elsewhere it opens full screen.
            entry<Route.DeleteAccount>(metadata = formDialogMetadata) {
                DeleteAccountScreen(isDialog = usesFormDialog)
            }
            // Why: a tablet keeps the home behind the new group; elsewhere it opens full screen.
            entry<Route.CreateGroup>(metadata = formDialogMetadata) {
                CreateGroupScreen(isDialog = usesFormDialog)
            }
        },
    )
}
