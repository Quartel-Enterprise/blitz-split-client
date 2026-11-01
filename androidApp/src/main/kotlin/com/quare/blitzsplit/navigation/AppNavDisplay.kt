package com.quare.blitzsplit.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberListDetailLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberUsesFormDialog
import com.quare.blitzsplit.android.designsystem.layout.rememberUsesGroupDialog
import com.quare.blitzsplit.android.feature.billdetail.BillDetailScreen
import com.quare.blitzsplit.android.feature.contacts.AgendaScreen
import com.quare.blitzsplit.android.feature.contacts.ContactInvitePresentation
import com.quare.blitzsplit.android.feature.contacts.ContactInviteScreen
import com.quare.blitzsplit.android.feature.contacts.ContactProfileScreen
import com.quare.blitzsplit.android.feature.contacts.ContactsScreen
import com.quare.blitzsplit.android.feature.contacts.rememberContactInvitePresentation
import com.quare.blitzsplit.android.feature.creategroup.CreateGroupScreen
import com.quare.blitzsplit.android.feature.deleteaccount.DeleteAccountScreen
import com.quare.blitzsplit.android.feature.editgroup.DefaultSplitScreen
import com.quare.blitzsplit.android.feature.editgroup.EditGroupScreen
import com.quare.blitzsplit.android.feature.editprofile.EditProfileScreen
import com.quare.blitzsplit.android.feature.globalbalances.GlobalBalancesScreen
import com.quare.blitzsplit.android.feature.groupdetail.BalancesScreen
import com.quare.blitzsplit.android.feature.groupdetail.GroupDetailScreen
import com.quare.blitzsplit.android.feature.groups.GroupsScreen
import com.quare.blitzsplit.android.feature.groups.HomeScreen
import com.quare.blitzsplit.android.feature.groupstats.GroupStatsScreen
import com.quare.blitzsplit.android.feature.invite.InviteScreen
import com.quare.blitzsplit.android.feature.join.JoinInviteScreen
import com.quare.blitzsplit.android.feature.join.JoinPickScreen
import com.quare.blitzsplit.android.feature.join.JoinWithLinkScreen
import com.quare.blitzsplit.android.feature.members.MembersScreen
import com.quare.blitzsplit.android.feature.notificationsettings.NotificationSettingsScreen
import com.quare.blitzsplit.android.feature.paymentkey.PaymentKeyScreen
import com.quare.blitzsplit.android.feature.profile.ProfileScreen
import com.quare.blitzsplit.android.feature.profilesetup.ProfileSetupScreen
import com.quare.blitzsplit.android.feature.recoverycode.RecoveryCodeSheet
import com.quare.blitzsplit.android.feature.registerpayment.PickPaymentScreen
import com.quare.blitzsplit.android.feature.registerpayment.RegisterPaymentScreen
import com.quare.blitzsplit.android.feature.restore.RestoreScreen
import com.quare.blitzsplit.android.feature.splitbill.SplitBillScreen
import com.quare.blitzsplit.android.feature.welcome.WelcomeScreen
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.tabGroupIdOrNull
import com.quare.blitzsplit.shared.app.AppUiState
import com.quare.blitzsplit.shared.app.AppViewModel
import org.koin.compose.viewmodel.koinViewModel

/*
 * Why: the back stack lives in :core:navigation and the ViewModels move it; Navigation 3 only
 * draws its entries, so Android walks the same flow as SwiftUI and React.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
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
    // Why: every dialog route keeps what was typed on a stray tap outside and sizes itself.
    val dialogProperties = DialogProperties(
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
    )
    val usesFormDialog = rememberUsesFormDialog()
    val formDialogMetadata = if (usesFormDialog) {
        FoldDialogSceneStrategy.dialogMetadataOf(dialogProperties)
    } else {
        emptyMap()
    }
    /*
     * Why: the canvas draws Novo grupo as a dialog over the home on anything wider than a phone: 560dp on a tablet and
     * a foldable upright, in a book's right half and in a tabletop's top half, never over the hinge.
     */
    val formFactor = rememberFoldLayout().formFactor
    val usesCreateGroupDialog = formFactor != FormFactor.Phone && formFactor != FormFactor.PhoneLandscape
    val createGroupMetadata = if (usesCreateGroupDialog) {
        FoldDialogSceneStrategy.dialogMetadataOf(
            properties = dialogProperties,
            isTopHalfOnTabletop = true,
        )
    } else {
        emptyMap()
    }
    val usesBillDialog = rememberUsesGroupDialog()
    val billDialogMetadata = if (usesBillDialog) {
        FoldDialogSceneStrategy.dialogMetadataOf(dialogProperties)
    } else {
        emptyMap()
    }
    val contactInvitePresentation = rememberContactInvitePresentation()
    val contactInviteMetadata = when (contactInvitePresentation) {
        ContactInvitePresentation.BottomSheet -> BottomSheetSceneStrategy.bottomSheet

        ContactInvitePresentation.Dialog -> FoldDialogSceneStrategy.dialogMetadataOf(
            DialogProperties(
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
        )

        ContactInvitePresentation.TabletopHalves -> emptyMap()
    }
    /*
     * Why: the home says whether it has no group at all, which only the book draws beside an empty detail. It can't
     * say so while a route covers it, so a group's tab on the stack (one just joined) counts as a group already.
     */
    var isGroupsScreenEmpty by rememberSaveable { mutableStateOf(false) }
    val isHomeEmpty = isGroupsScreenEmpty && backStack.none { tabGroupIdOrNull(it) != null }
    val listDetail = homeListDetailOf(
        listDetail = rememberListDetailLayout(),
        isHomeEmpty = isHomeEmpty,
    )
    val listDetailStrategy = rememberHomeListDetailStrategy(listDetail)
    val groupsMetadata = remember(listDetail, isHomeEmpty) {
        homeListPaneMetadataOf(
            listDetail = listDetail,
            isHomeEmpty = isHomeEmpty,
        )
    }
    NavDisplay(
        backStack = backStack,
        onBack = { appViewModel.navigator.navigateBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(rememberSaveableStateHolder()),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        // Why: a dialog or a sheet over the group keeps the list and detail drawn under it.
        sceneStrategies = listOf(FoldDialogSceneStrategy(), BottomSheetSceneStrategy(), listDetailStrategy),
        entryProvider = entryProvider {
            entry<Route.Welcome> { WelcomeScreen() }
            entry<Route.ProfileSetup> { ProfileSetupScreen() }
            entry<Route.Restore> { RestoreScreen() }
            // Why: Início, the root of the home; the other tabs open right above it.
            entry<Route.Home> { HomeScreen(recoveryCodeSheet = { RecoveryCodeSheet() }) }
            // Why: on a tablet lying down and an unfolded book, the list beside the group open over it.
            entry<Route.Groups>(metadata = groupsMetadata) {
                GroupsScreen(
                    recoveryCodeSheet = { RecoveryCodeSheet() },
                    onEmptyChanged = { isGroupsScreenEmpty = it },
                )
            }
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
            // Why: a dialog over the home wider than a phone; a phone opens it full screen, in two columns lying down.
            entry<Route.CreateGroup>(metadata = createGroupMetadata) {
                CreateGroupScreen(isDialog = usesCreateGroupDialog)
            }
            // Why: it takes the new group's place; a tablet keeps the home behind it, elsewhere it opens full screen.
            entry<Route.Invite>(metadata = formDialogMetadata) { route ->
                InviteScreen(
                    route = route,
                    isDialog = usesFormDialog,
                )
            }
            // Why: the group's tabs go in the pane beside the groups, where the window has room for it.
            entry<Route.GroupDetail>(metadata = homeDetailPaneMetadata) { route ->
                InGroupPane { GroupDetailScreen(route) }
            }
            // Why: a tablet keeps the group behind the form; elsewhere, foldables included, it opens full screen.
            entry<Route.EditGroup>(metadata = formDialogMetadata) { route ->
                EditGroupScreen(
                    route = route,
                    isDialog = usesFormDialog,
                )
            }
            // Why: over Editar grupo, the same way: a dialog on a tablet, full screen elsewhere (two columns lying down).
            entry<Route.DefaultSplit>(metadata = formDialogMetadata) { route ->
                DefaultSplitScreen(
                    route = route,
                    isDialog = usesFormDialog,
                )
            }
            // Why: a tablet and the upright foldable keep the group behind the bill; elsewhere it opens full screen.
            entry<Route.SplitBill>(metadata = billDialogMetadata) { route ->
                SplitBillScreen(
                    route = route,
                    isDialog = usesBillDialog,
                )
            }
            // Why: like Rachar conta, a tablet and the upright foldable keep the group behind the bill.
            entry<Route.BillDetail>(metadata = billDialogMetadata) { route ->
                BillDetailScreen(
                    route = route,
                    isDialog = usesBillDialog,
                )
            }
            // Why: a tab of the group, which takes Despesas' or Membros' place, beside the groups too.
            entry<Route.Balances>(metadata = homeDetailPaneMetadata) { route ->
                InGroupPane { BalancesScreen(route) }
            }
            entry<Route.GroupStats> { route -> GroupStatsScreen(route) }
            // Why: a sheet over the group on a phone; like Conta, a dialog on a tablet and the upright foldable.
            entry<Route.RegisterPayment>(
                metadata = if (usesBillDialog) billDialogMetadata else BottomSheetSceneStrategy.bottomSheet,
            ) { route ->
                RegisterPaymentScreen(
                    route = route,
                    isDialog = usesBillDialog,
                )
            }
            // Why: "Quem pagou a quem?" from Início, opened the way Registrar pagamento opens, whose place it gives.
            entry<Route.PickPayment>(
                metadata = if (usesBillDialog) billDialogMetadata else BottomSheetSceneStrategy.bottomSheet,
            ) {
                PickPaymentScreen(isDialog = usesBillDialog)
            }
            // Why: the home's Saldos tab, over Início.
            entry<Route.GlobalBalances> { route -> GlobalBalancesScreen(route) }
            // Why: Acertar and Recebi of Saldos open Registrar pagamento across groups, the same way as over a group.
            entry<Route.SettleUp>(
                metadata = if (usesBillDialog) billDialogMetadata else BottomSheetSceneStrategy.bottomSheet,
            ) { route ->
                RegisterPaymentScreen(
                    route = route,
                    isDialog = usesBillDialog,
                )
            }
            // Why: the home's Contatos tab, over the groups; a wide window draws the profile or the agenda beside it.
            entry<Route.Contacts> { ContactsScreen() }
            entry<Route.ContactProfile> { route -> ContactProfileScreen(route) }
            entry<Route.ContactsAgenda> { AgendaScreen() }
            // Why: a sheet on a phone, a dialog elsewhere, and the code above the hinge on a tabletop.
            entry<Route.ContactInvite>(metadata = contactInviteMetadata) { route ->
                ContactInviteScreen(
                    route = route,
                    presentation = contactInvitePresentation,
                )
            }
            entry<Route.Members>(metadata = homeDetailPaneMetadata) { route ->
                InGroupPane { MembersScreen(route) }
            }
            entry<Route.JoinInvite> { route -> JoinInviteScreen(route) }
            entry<Route.JoinPick> { route -> JoinPickScreen(route) }
            entry<Route.JoinWithLink> { route -> JoinWithLinkScreen(route) }
        },
    )
}
