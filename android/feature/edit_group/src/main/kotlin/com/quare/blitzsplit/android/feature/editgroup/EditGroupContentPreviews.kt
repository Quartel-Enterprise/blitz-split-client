package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.previewInitialsOf
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.DebtSimplificationSummary
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiState
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyAmount
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyDebtsConfirmation
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyName
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyPersonChange
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyQueuedPayment

@DevicePreviews
@Composable
private fun EditGroupPreview() {
    BlitzSplitTheme {
        EditGroupContent(
            state = buildPreviewState(
                name = "Viagem Floripa",
                isOnline = true,
            ),
            isDialog = rememberUsesEditGroupDialog(),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun EditGroupLoadingPreview() {
    BlitzSplitTheme {
        EditGroupContent(
            state = buildPreviewState(
                name = "",
                isOnline = true,
            ).copy(isLoaded = false),
            isDialog = rememberUsesEditGroupDialog(),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun EditGroupEmptyNameOfflinePreview() {
    BlitzSplitTheme {
        EditGroupContent(
            state = buildPreviewState(
                name = "",
                isOnline = false,
            ),
            isDialog = rememberUsesEditGroupDialog(),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun EditGroupSimplifiedPreview() {
    BlitzSplitTheme {
        EditGroupContent(
            state = buildPreviewState(
                name = "Viagem Floripa",
                isOnline = true,
            ).copy(
                simplifiesDebts = true,
                simplification = DebtSimplificationSummary(
                    paymentCount = 5,
                    unsimplifiedPaymentCount = 7,
                ),
            ),
            isDialog = rememberUsesEditGroupDialog(),
            onEvent = {},
        )
    }
}

// Why: 13.1c, two people are already a single payment.
@DevicePreviews
@Composable
private fun EditGroupSimplifyTwoPeoplePreview() {
    BlitzSplitTheme {
        EditGroupContent(
            state = buildPreviewState(
                name = "Apê 302",
                isOnline = true,
            ).copy(memberCount = 2),
            isDialog = rememberUsesEditGroupDialog(),
            onEvent = {},
        )
    }
}

// Why: 13.1d, turned on offline and waiting to go up.
@DevicePreviews
@Composable
private fun EditGroupSimplifyOfflinePreview() {
    BlitzSplitTheme {
        EditGroupContent(
            state = buildPreviewState(
                name = "Viagem Floripa",
                isOnline = false,
            ).copy(
                simplifiesDebts = true,
                isSimplifyDebtsPending = true,
            ),
            isDialog = rememberUsesEditGroupDialog(),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun SimplifyDebtsDialogOnPreview() {
    BlitzSplitTheme {
        SimplifyDebtsDialog(
            confirmation = buildPreviewConfirmation(isTurningOn = true),
            currency = GroupCurrency.BRL,
            isChanging = false,
            onEvent = {},
        )
    }
}

// Why: 13.3b, turning it off names only what changes for this device's user.
@DevicePreviews
@Composable
private fun SimplifyDebtsDialogOffPreview() {
    BlitzSplitTheme {
        SimplifyDebtsDialog(
            confirmation = buildPreviewConfirmation(isTurningOn = false).copy(
                paymentsBefore = 5,
                paymentsAfter = 7,
                people = listOf(
                    buildPreviewChange(
                        name = "Pierre",
                        isMe = true,
                        stopsPaying = emptyList(),
                        pays = listOf(SimplifyAmount(buildPreviewName("Bruno"), 11_800L)),
                    ),
                ),
                othersChangingCount = 5,
            ),
            currency = GroupCurrency.BRL,
            isChanging = false,
            onEvent = {},
        )
    }
}

// Why: 13.3c and 13.3d, a group already at its fewest payments, with one payment in the queue.
@DevicePreviews
@Composable
private fun SimplifyDebtsDialogMinimalPreview() {
    BlitzSplitTheme {
        SimplifyDebtsDialog(
            confirmation = buildPreviewConfirmation(isTurningOn = true).copy(
                groupName = "Futebol de quinta",
                paymentsBefore = 2,
                paymentsAfter = 2,
                people = emptyList(),
                queuedPayments = listOf(
                    SimplifyQueuedPayment(
                        from = buildPreviewName("Elisa"),
                        to = SimplifyName(
                            name = "Pierre",
                            isMe = true,
                        ),
                        amountMinor = 5_000L,
                    ),
                ),
            ),
            currency = GroupCurrency.BRL,
            isChanging = false,
            onEvent = {},
        )
    }
}

private fun buildPreviewName(name: String): SimplifyName = SimplifyName(
    name = name,
    isMe = false,
)

private fun buildPreviewChange(
    name: String,
    isMe: Boolean,
    stopsPaying: List<SimplifyAmount>,
    pays: List<SimplifyAmount>,
    isGuest: Boolean = false,
    receivesFrom: List<SimplifyName> = emptyList(),
): SimplifyPersonChange = SimplifyPersonChange(
    memberId = name,
    person = SimplifyName(
        name = name,
        isMe = isMe,
    ),
    initials = previewInitialsOf(name),
    avatarColor = AvatarColor.GREEN,
    photoUrl = null,
    isGuest = isGuest,
    stopsPaying = stopsPaying,
    pays = pays,
    stopsReceivingFrom = emptyList(),
    receivesFrom = receivesFrom,
)

private fun buildPreviewConfirmation(isTurningOn: Boolean): SimplifyDebtsConfirmation = SimplifyDebtsConfirmation(
    isTurningOn = isTurningOn,
    groupName = "Viagem Floripa",
    paymentsBefore = 7,
    paymentsAfter = 5,
    people = listOf(
        buildPreviewChange(
            name = "Pierre",
            isMe = true,
            stopsPaying = listOf(SimplifyAmount(buildPreviewName("Bruno"), 11_800L)),
            pays = emptyList(),
            receivesFrom = listOf(buildPreviewName("Carla"), buildPreviewName("Diego"), buildPreviewName("Elisa")),
        ),
        buildPreviewChange(
            name = "Ana",
            isMe = false,
            stopsPaying = emptyList(),
            pays = listOf(SimplifyAmount(buildPreviewName("Bruno"), 20_380L)),
        ),
        buildPreviewChange(
            name = "Diego",
            isMe = false,
            stopsPaying = listOf(SimplifyAmount(buildPreviewName("Bruno"), 6_400L)),
            pays = listOf(
                SimplifyAmount(
                    to = SimplifyName(
                        name = "Pierre",
                        isMe = true,
                    ),
                    amountMinor = 6_400L,
                ),
            ),
            isGuest = true,
        ),
    ),
    othersChangingCount = 0,
    queuedPayments = emptyList(),
)

private fun buildPreviewState(
    name: String,
    isOnline: Boolean,
): EditGroupUiState = EditGroupUiState(
    name = name,
    initials = if (name.isEmpty()) "" else "VF",
    color = AvatarColor.PEACH,
    photo = null,
    isPreparingPhoto = false,
    showsPhotoError = false,
    iconEditor = null,
    photoCrop = null,
    type = GroupType.TRIP,
    currency = GroupCurrency.BRL,
    isCurrencyLocked = true,
    defaultSplitMode = DefaultSplitMode.SHARES,
    isOnline = isOnline,
    isLoaded = true,
    isSaving = false,
    showsSaveError = false,
    hasChanges = true,
    simplifiesDebts = false,
    isSimplifyDebtsPending = false,
    simplification = null,
    memberCount = 6,
    simplifyConfirmation = null,
    isChangingSimplifyDebts = false,
    simplifyConflict = null,
)
