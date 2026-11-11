package com.quare.blitzsplit.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import com.quare.blitzsplit.android.designsystem.component.HomeBarsLayout
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.FormPresentation
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberFormPresentation
import com.quare.blitzsplit.android.designsystem.layout.rememberListDetailLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberUsesFormDialog
import com.quare.blitzsplit.android.feature.activity.ActivityScreen
import com.quare.blitzsplit.android.feature.billdetail.BillDetailScreen
import com.quare.blitzsplit.android.feature.billdetail.ShareBillScreen
import com.quare.blitzsplit.android.feature.categorymap.CategoryMapScreen
import com.quare.blitzsplit.android.feature.contacts.AgendaScreen
import com.quare.blitzsplit.android.feature.contacts.ContactInviteScreen
import com.quare.blitzsplit.android.feature.contacts.ContactProfileScreen
import com.quare.blitzsplit.android.feature.contacts.ContactsScreen
import com.quare.blitzsplit.android.feature.contacts.ProfileLinkScreen
import com.quare.blitzsplit.android.feature.creategroup.CreateGroupScreen
import com.quare.blitzsplit.android.feature.deleteaccount.DeleteAccountScreen
import com.quare.blitzsplit.android.feature.editgroup.DefaultSplitScreen
import com.quare.blitzsplit.android.feature.editgroup.EditGroupScreen
import com.quare.blitzsplit.android.feature.editgroup.GroupPhotoScreen
import com.quare.blitzsplit.android.feature.editgroup.SimplifyDebtsInfoScreen
import com.quare.blitzsplit.android.feature.editgroup.rememberUsesDefaultSplitDialog
import com.quare.blitzsplit.android.feature.editgroup.rememberUsesEditGroupDialog
import com.quare.blitzsplit.android.feature.editprofile.EditProfileScreen
import com.quare.blitzsplit.android.feature.editprofile.ProfilePhotoScreen
import com.quare.blitzsplit.android.feature.globalbalances.ChargeScreen
import com.quare.blitzsplit.android.feature.globalbalances.GlobalBalancesScreen
import com.quare.blitzsplit.android.feature.groupdetail.GroupDetailScreen
import com.quare.blitzsplit.android.feature.groups.GroupsScreen
import com.quare.blitzsplit.android.feature.groups.HomeScreen
import com.quare.blitzsplit.android.feature.groups.SearchScreen
import com.quare.blitzsplit.android.feature.groupstats.CategoryExpensesScreen
import com.quare.blitzsplit.android.feature.groupstats.GroupStatsScreen
import com.quare.blitzsplit.android.feature.invite.InviteScreen
import com.quare.blitzsplit.android.feature.join.JoinInviteScreen
import com.quare.blitzsplit.android.feature.join.JoinPickScreen
import com.quare.blitzsplit.android.feature.join.JoinWithLinkScreen
import com.quare.blitzsplit.android.feature.legal.LegalDocumentScreen
import com.quare.blitzsplit.android.feature.members.rememberMembersTab
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
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.groupScreenIdOrNull
import com.quare.blitzsplit.core.navigation.navigateBackFrom
import com.quare.blitzsplit.push.PushSetup
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
    val isWiderThanPhone = formFactor != FormFactor.Phone && formFactor != FormFactor.PhoneLandscape
    val usesCreateGroupDialog = isWiderThanPhone
    val createGroupMetadata = if (usesCreateGroupDialog) {
        FoldDialogSceneStrategy.dialogMetadataOf(
            properties = dialogProperties,
            isTopHalfOnTabletop = true,
        )
    } else {
        emptyMap()
    }
    /*
     * Why: like a payment key, a tablet keeps the profile behind Excluir conta; a book and a tabletop, which open the
     * profile's confirmations in one half (4.6, 4.9), open it in the right half too; elsewhere it is full screen.
     */
    val usesDeleteAccountDialog = usesFormDialog || formFactor == FormFactor.Book || formFactor == FormFactor.Tabletop
    val deleteAccountMetadata = if (usesDeleteAccountDialog) {
        FoldDialogSceneStrategy.dialogMetadataOf(dialogProperties)
    } else {
        emptyMap()
    }
    val usesEditGroupDialog = rememberUsesEditGroupDialog()
    val editGroupMetadata = if (usesEditGroupDialog) {
        FoldDialogSceneStrategy.dialogMetadataOf(dialogProperties)
    } else {
        emptyMap()
    }
    val usesDefaultSplitDialog = rememberUsesDefaultSplitDialog()
    val defaultSplitMetadata = if (usesDefaultSplitDialog) {
        FoldDialogSceneStrategy.dialogMetadataOf(dialogProperties)
    } else {
        emptyMap()
    }
    /*
     * Why: flow 7 draws Rachar conta and Conta as a dialog over the group anywhere wider than a phone: 560dp on a tablet
     * and a foldable upright, 920dp on a tablet lying down, in a book's right half with the group under the scrim. A
     * tabletop splits a new bill on its hinge, full screen, and draws Conta and Editar conta as the book does.
     */
    val opensBillOverGroup = isWiderThanPhone
    val billOverGroupMetadata = if (opensBillOverGroup) {
        FoldDialogSceneStrategy.dialogMetadataOf(dialogProperties)
    } else {
        emptyMap()
    }
    val splitsBillAsDialog = { route: Route.SplitBill ->
        opensBillOverGroup && !(formFactor == FormFactor.Tabletop && route.expenseId == null)
    }
    /*
     * Why: Registrar pagamento, Convidar of Contatos and Edit profile open the same way: a sheet on a phone, a dialog
     * wider (a book's right half) and full screen on a tabletop, the first two with their content split on its hinge
     * and Edit profile in the screen it always drew there.
     */
    val formPresentation = rememberFormPresentation()
    val formPresentationMetadata = when (formPresentation) {
        FormPresentation.BottomSheet -> BottomSheetSceneStrategy.bottomSheet
        FormPresentation.Dialog -> FoldDialogSceneStrategy.dialogMetadataOf(dialogProperties)
        FormPresentation.TabletopHalves -> emptyMap()
    }
    // Why: a dialog wherever Registrar pagamento isn't a sheet, a tabletop's in its half, as short as it is.
    val usesPickPaymentDialog = formPresentation != FormPresentation.BottomSheet
    val pickPaymentMetadata = if (usesPickPaymentDialog) {
        FoldDialogSceneStrategy.dialogMetadataOf(dialogProperties)
    } else {
        BottomSheetSceneStrategy.bottomSheet
    }
    /*
     * Why: the home says whether it has no group at all, which only the book draws beside an empty detail. It can't
     * say so while a route covers it, so a group's tab on the stack (one just joined) counts as a group already.
     */
    var isGroupsScreenEmpty by rememberSaveable { mutableStateOf(false) }
    val isHomeEmpty = isGroupsScreenEmpty && backStack.none { groupScreenIdOrNull(it) != null }
    /*
     * Why: a tabletop lying down draws the group's Saldos alone, full screen, its balance above the hinge and the pairs
     * under it (6.2); its other tabs stay beside the list, as on a book. Only the group on top counts.
     */
    var openGroupTab by remember { mutableStateOf<Pair<Route.GroupDetail, GroupTab>?>(null) }
    val topGroupRoute = backStack.lastOrNull { it is Route.GroupDetail }
    // Why: a group closed forgets its tab, so opening it again never starts as the Saldos it was left on.
    LaunchedEffect(topGroupRoute) {
        if (openGroupTab?.first != topGroupRoute) openGroupTab = null
    }
    val showsBalancesAlone = formFactor == FormFactor.Tabletop &&
        openGroupTab?.let { (route, tab) -> tab == GroupTab.BALANCES && topGroupRoute == route } == true
    val listDetail = homeListDetailOf(
        listDetail = rememberListDetailLayout().takeUnless { showsBalancesAlone },
        isHomeEmpty = isHomeEmpty,
    )
    val listDetailStrategy = rememberHomeListDetailStrategy(listDetail)
    val groupStatsMetadata = if (listDetail?.isBook == false) homeDetailPaneMetadata else emptyMap()
    // Why: NavDisplay keeps an entry's metadata until the back stack changes, so the placeholder reads it as it draws.
    val currentIsHomeEmpty by rememberUpdatedState(isHomeEmpty)
    val groupsMetadata = remember(listDetail) {
        homeListPaneMetadataOf(
            listDetail = listDetail,
            isHomeEmpty = { currentIsHomeEmpty },
        ) + homeTabMetadata
    }
    // Why: stateless, so built once instead of on every recomposition of the navigation.
    val transitionSpec =
        remember {
            createHomeTabTransitionSpec(
                isPop = false,
                default = defaultTransitionSpec<Route>(),
            )
        }
    val popTransitionSpec =
        remember {
            createHomeTabTransitionSpec(
                isPop = true,
                default = defaultPopTransitionSpec<Route>(),
            )
        }
    val predictivePopTransitionSpec = remember {
        createHomeTabPredictivePopTransitionSpec(defaultPredictivePopTransitionSpec<Route>())
    }
    val entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(rememberSaveableStateHolder()),
        rememberViewModelStoreNavEntryDecorator(),
        homeBarsEntryDecorator,
    )
    val entries = rememberAppNavEntries(
        backStack = backStack,
        decorators = entryDecorators,
        entryProvider = entryProvider {
            entry<Route.Welcome> { WelcomeScreen() }
            entry<Route.ProfileSetup> { ProfileSetupScreen() }
            entry<Route.Restore> { RestoreScreen() }
            // Why: Início, the root of the home; the other tabs open right above it.
            entry<Route.Home>(metadata = homeTabMetadata) {
                HomeScreen(
                    recoveryCodeSheet = { RecoveryCodeSheet() },
                    isPushConfigured = PushSetup.isConfigured,
                )
            }
            // Why: on a tablet lying down and an unfolded book, the list beside the group open over it.
            entry<Route.Groups>(metadata = groupsMetadata) {
                GroupsScreen(
                    recoveryCodeSheet = { RecoveryCodeSheet() },
                    onEmptyChanged = { isGroupsScreenEmpty = it },
                )
            }
            // Why: "Buscar" over Início, Saldos, Contatos or Atividade, full screen as Grupos' own takes its place.
            entry<Route.Search>(metadata = homeTabMetadata) { SearchScreen() }
            entry<Route.Profile> { ProfileScreen(recoveryCodeSheet = { RecoveryCodeSheet() }) }
            // Why: a sheet over the profile on a phone, a dialog wider (a book's right half), full screen on a tabletop.
            entry<Route.EditProfile>(metadata = formPresentationMetadata) {
                // Why: an open scene keeps the way it opened when the fold changes under it, so its content does too.
                val presentation = remember { formPresentation }
                EditProfileScreen(presentation = presentation)
            }
            // Why: the photo big over the profile, the whole window dimmed behind it (a book's end half, as any dialog).
            entry<Route.ProfilePhoto>(
                metadata = FoldDialogSceneStrategy.dialogMetadataOf(
                    DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false,
                    ),
                ),
            ) {
                ProfilePhotoScreen()
            }
            // Why: the group's photo big over the group, as the profile's, the whole window dimmed behind it.
            entry<Route.GroupPhoto>(
                metadata = FoldDialogSceneStrategy.dialogMetadataOf(
                    DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false,
                    ),
                ),
            ) { route ->
                GroupPhotoScreen(route = route)
            }
            entry<Route.NotificationSettings> { NotificationSettingsScreen() }
            entry<Route.CategoryMap> { CategoryMapScreen() }
            entry<Route.LegalDocument> { route -> LegalDocumentScreen(route = route) }
            // Why: a tablet keeps the profile behind the key, a phone or an unfolded foldable opens it full screen.
            entry<Route.PaymentKey>(metadata = formDialogMetadata) { route ->
                PaymentKeyScreen(
                    route = route,
                    isDialog = usesFormDialog,
                )
            }
            entry<Route.DeleteAccount>(metadata = deleteAccountMetadata) {
                DeleteAccountScreen(isDialog = usesDeleteAccountDialog)
            }
            // Why: a dialog over the home wider than a phone; a phone opens it full screen, in two columns lying down.
            entry<Route.CreateGroup>(metadata = createGroupMetadata) {
                CreateGroupScreen(isDialog = usesCreateGroupDialog)
            }
            // Why: it takes the new group's place, full screen at every size (decision 1), laid out per size inside.
            entry<Route.Invite> { route -> InviteScreen(route) }
            // Why: the group, its tabs inside it, goes in the pane beside the groups, where the window has room for it.
            entry<Route.GroupDetail>(metadata = homeDetailPaneMetadata) { route ->
                InGroupPane {
                    GroupDetailScreen(
                        route = route,
                        membersTab = { snackbarHostState -> rememberMembersTab(route, snackbarHostState) },
                        onTabChanged = { tab -> openGroupTab = route to tab },
                    )
                }
            }
            // Why: wider than a phone the group stays behind the form, in a book's right half on a foldable.
            entry<Route.EditGroup>(metadata = editGroupMetadata) { route ->
                EditGroupScreen(
                    route = route,
                    isDialog = usesEditGroupDialog,
                )
            }
            // Why: over Editar grupo: a dialog on a tablet and a book, full screen on a phone and a foldable upright.
            entry<Route.DefaultSplit>(metadata = defaultSplitMetadata) { route ->
                DefaultSplitScreen(
                    route = route,
                    isDialog = usesDefaultSplitDialog,
                )
            }
            // Why: "Como funciona" (13.2), full screen over Editar grupo or the group's Saldos at every size.
            entry<Route.SimplifyDebtsInfo> { route ->
                SimplifyDebtsInfoScreen(
                    route = route,
                    onBack = { appViewModel.navigator.navigateBackFrom(route) },
                )
            }
            // Why: the group stays behind the bill wider than a phone, but under a new bill on a tabletop's hinge.
            entry<Route.SplitBill>(
                metadata = { route -> if (splitsBillAsDialog(route)) billOverGroupMetadata else emptyMap() },
            ) { route ->
                SplitBillScreen(
                    route = route,
                    isDialog = splitsBillAsDialog(route),
                )
            }
            // Why: like Rachar conta, the group stays behind the bill wider than a phone, a tabletop included.
            entry<Route.BillDetail>(metadata = billOverGroupMetadata) { route ->
                BillDetailScreen(
                    route = route,
                    isDialog = opensBillOverGroup,
                )
            }
            // Why: "Compartilhar conta" over Conta, opened as Convidar of Contatos: a sheet, a dialog or the tabletop's halves.
            entry<Route.ShareBill>(metadata = formPresentationMetadata) { route ->
                val presentation = remember { formPresentation }
                ShareBillScreen(
                    route = route,
                    presentation = presentation,
                )
            }
            /*
             * Why: a tablet lying down draws it in the group's pane beside the groups, with its back arrow (decision 2);
             * a book and everywhere else cover the window, the book split on its hinge.
             */
            entry<Route.GroupStats>(metadata = groupStatsMetadata) { route ->
                InGroupPane { GroupStatsScreen(route) }
            }
            // Why: a category's bills, from the statistics, in the same place as them (the group's pane beside the groups).
            entry<Route.CategoryExpenses>(metadata = groupStatsMetadata) { route ->
                InGroupPane { CategoryExpensesScreen(route) }
            }
            // Why: a sheet over the group on a phone, a dialog wider (a book's right half), full screen on a tabletop.
            entry<Route.RegisterPayment>(metadata = formPresentationMetadata) { route ->
                /*
                 * Why: an open scene keeps the way it opened when the fold changes under it (a book's dialog stays a
                 * dialog on a tabletop), so its content keeps that way too.
                 */
                val presentation = remember { formPresentation }
                RegisterPaymentScreen(
                    route = route,
                    presentation = presentation,
                )
            }
            // Why: "Quem pagou a quem?" from Início, opened the way Registrar pagamento opens, whose place it gives.
            entry<Route.PickPayment>(metadata = pickPaymentMetadata) {
                val isDialog = remember { usesPickPaymentDialog }
                PickPaymentScreen(isDialog = isDialog)
            }
            // Why: the home's Saldos tab, over Início.
            entry<Route.GlobalBalances>(metadata = homeTabMetadata) { route -> GlobalBalancesScreen(route) }
            // Why: Cobrar over the screen that opened it (Saldos, a contact's profile), as short as Quem pagou quem.
            entry<Route.Charge>(metadata = pickPaymentMetadata) { route ->
                val isDialog = remember { usesPickPaymentDialog }
                ChargeScreen(
                    route = route,
                    isDialog = isDialog,
                )
            }
            // Why: Atividade, pushed over Início by its bell or over a contact's profile by their history.
            entry<Route.Activity> { route -> ActivityScreen(route) }
            // Why: Acertar and Recebi of Saldos open Registrar pagamento across groups, the same way as over a group.
            entry<Route.SettleUp>(metadata = formPresentationMetadata) { route ->
                val presentation = remember { formPresentation }
                RegisterPaymentScreen(
                    route = route,
                    presentation = presentation,
                )
            }
            // Why: the home's Contatos tab, over the groups; a wide window draws the profile or the agenda beside it.
            entry<Route.Contacts>(metadata = homeTabMetadata) { ContactsScreen() }
            entry<Route.ContactProfile> { route -> ContactProfileScreen(route) }
            // Why: a profile link opened, until it gives way to the contact's profile or Profile.
            entry<Route.ProfileLink> { route -> ProfileLinkScreen(route) }
            entry<Route.ContactsAgenda> { AgendaScreen() }
            // Why: a sheet on a phone, a dialog elsewhere, and the code above the hinge on a tabletop.
            entry<Route.ContactInvite>(metadata = formPresentationMetadata) { route ->
                val presentation = remember { formPresentation }
                ContactInviteScreen(
                    route = route,
                    presentation = presentation,
                )
            }
            entry<Route.JoinInvite> { route -> JoinInviteScreen(route) }
            entry<Route.JoinPick> { route -> JoinPickScreen(route) }
            entry<Route.JoinWithLink> { route -> JoinWithLinkScreen(route) }
        },
    )
    // Why: the home's bar or rail, drawn once around the navigation, so switching tabs swaps only the content.
    HomeBarsLayout(stackKeys = entries.map { it.contentKey }) {
        NavDisplay(
            entries = entries,
            // Why: a dialog or a sheet over the group keeps the list and detail drawn under it.
            sceneStrategies = listOf(FoldDialogSceneStrategy(), BottomSheetSceneStrategy(), listDetailStrategy),
            transitionSpec = transitionSpec,
            popTransitionSpec = popTransitionSpec,
            predictivePopTransitionSpec = predictivePopTransitionSpec,
            onBack = { appViewModel.navigator.navigateBack() },
        )
    }
}
