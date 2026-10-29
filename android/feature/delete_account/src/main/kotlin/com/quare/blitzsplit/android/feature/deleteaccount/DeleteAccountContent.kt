package com.quare.blitzsplit.android.feature.deleteaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.OfflineNoticeBanner
import com.quare.blitzsplit.android.designsystem.component.labelRes
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.feature.deleteaccount.presentation.model.DeleteAccountUiEvent
import com.quare.blitzsplit.feature.deleteaccount.presentation.model.DeleteAccountUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val singleColumnMaxWidth = 592.dp
private val dialogWidth = 560.dp
private val phoneLandscapeEndColumnWidth = 360.dp
private const val DISABLED_ALPHA = 0.38f

@Composable
fun DeleteAccountContent(
    state: DeleteAccountUiState,
    // Why: a tablet draws it as a 560dp dialog over the profile, so its layout is always one column.
    isDialog: Boolean,
    onEvent: (DeleteAccountUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isDialog) {
        DeleteAccountDialogSurface(
            state = state,
            onEvent = onEvent,
            modifier = modifier,
        )
    } else {
        DeleteAccountFullScreenScaffold(
            state = state,
            onEvent = onEvent,
            modifier = modifier,
        )
    }
}

@Composable
private fun DeleteAccountFullScreenScaffold(
    state: DeleteAccountUiState,
    onEvent: (DeleteAccountUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DeleteAccountTopBar(
                onEvent = onEvent,
                isDialog = false,
            )
        },
    ) { padding ->
        AdaptiveScreen(modifier = Modifier.fillMaxSize().padding(padding)) { layout ->
            if (layout.usesTwoColumns) {
                DeleteAccountTwoColumnLayout(
                    state = state,
                    onEvent = onEvent,
                    isBookPosture = layout.isTablet,
                )
            } else {
                DeleteAccountSingleColumn(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    }
}

// Why: what gets deleted scrolls, while the confirmation and the buttons stay at the bottom, as the canvas draws.
@Composable
private fun DeleteAccountSingleColumn(
    state: DeleteAccountUiState,
    onEvent: (DeleteAccountUiEvent) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = singleColumnMaxWidth)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DeleteAccountDetailsSection(
                    state = state,
                    onEvent = onEvent,
                )
            }
            Column(
                modifier = Modifier.padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 12.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DeleteAccountActionsSection(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    }
}

/*
 * Why: lying down there is little height, so what gets deleted goes left and the confirmation right. On an
 * unfolded foldable as a book the two columns are equal, so the 72dp gap sits on the hinge.
 */
@Composable
private fun DeleteAccountTwoColumnLayout(
    state: DeleteAccountUiState,
    onEvent: (DeleteAccountUiEvent) -> Unit,
    isBookPosture: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = if (isBookPosture) 32.dp else 24.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isBookPosture) 72.dp else 40.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = if (isBookPosture) 24.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DeleteAccountDetailsSection(
                state = state,
                onEvent = onEvent,
            )
        }
        Column(
            modifier = Modifier
                .then(if (isBookPosture) Modifier.weight(1f) else Modifier.widthIn(max = phoneLandscapeEndColumnWidth))
                .verticalScroll(rememberScrollState())
                .padding(vertical = if (isBookPosture) 24.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DeleteAccountActionsSection(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun DeleteAccountDialogSurface(
    state: DeleteAccountUiState,
    onEvent: (DeleteAccountUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .widthIn(max = dialogWidth)
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            DeleteAccountTopBar(
                onEvent = onEvent,
                isDialog = true,
            )
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 24.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DeleteAccountDetailsSection(
                    state = state,
                    onEvent = onEvent,
                )
                Column(
                    modifier = Modifier.padding(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DeleteAccountActionsSection(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteAccountTopBar(
    onEvent: (DeleteAccountUiEvent) -> Unit,
    isDialog: Boolean,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.delete_account_title)) },
        navigationIcon = {
            IconButton(onClick = { onEvent(DeleteAccountUiEvent.OnBackClicked) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(DesignR.string.designsystem_back),
                )
            }
        },
        windowInsets = if (isDialog) WindowInsets(0) else TopAppBarDefaults.windowInsets,
        colors = if (isDialog) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        },
    )
}

@Composable
private fun ColumnScope.DeleteAccountDetailsSection(
    state: DeleteAccountUiState,
    onEvent: (DeleteAccountUiEvent) -> Unit,
) {
    if (state.showsOfflineNotice) {
        OfflineNoticeBanner(
            title = stringResource(R.string.delete_account_offline_title),
            message = stringResource(R.string.delete_account_offline_body),
            isCompact = true,
        )
    }
    DeleteAccountHeading()
    DeletedItemsCard(provider = state.accountProvider)
    // Why: the export reads the copy on this device, so it works offline too.
    TextButton(
        onClick = { onEvent(DeleteAccountUiEvent.OnExportClicked) },
        modifier = Modifier.align(Alignment.Start),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(stringResource(R.string.delete_account_export))
        }
    }
}

@Composable
private fun DeleteAccountHeading() {
    Row(
        modifier = Modifier.padding(
            start = 4.dp,
            end = 4.dp,
            top = 4.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                )
            }
        }
        Text(
            text = stringResource(R.string.delete_account_heading),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DeletedItemsCard(provider: AccountProvider?) {
    val items = buildList {
        add(stringResource(R.string.delete_account_deleted_profile))
        add(stringResource(R.string.delete_account_deleted_keys))
        provider?.let { add(stringResource(R.string.delete_account_deleted_provider, stringResource(it.labelRes))) }
        add(stringResource(R.string.delete_account_deleted_device))
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.delete_account_deleted_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items.forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteAccountActionsSection(
    state: DeleteAccountUiState,
    onEvent: (DeleteAccountUiEvent) -> Unit,
) {
    ConfirmRow(
        state = state,
        onEvent = onEvent,
    )
    if (state.hasFailed) {
        Text(
            text = stringResource(R.string.delete_account_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
    Button(
        onClick = { onEvent(DeleteAccountUiEvent.OnDeleteClicked) },
        enabled = state.canDelete,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
    ) {
        if (state.isDeleting) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = stringResource(R.string.delete_account_delete),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
    TextButton(
        onClick = { onEvent(DeleteAccountUiEvent.OnBackClicked) },
        enabled = !state.isDeleting,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) { Text(stringResource(R.string.delete_account_cancel)) }
}

// Why: offline the checkbox dims with the button, since deleting needs the server.
@Composable
private fun ConfirmRow(
    state: DeleteAccountUiState,
    onEvent: (DeleteAccountUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = state.hasConfirmed,
                enabled = state.isConfirmEnabled,
                role = Role.Checkbox,
                onValueChange = { onEvent(DeleteAccountUiEvent.OnConfirmChanged(it)) },
            ).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = state.hasConfirmed,
            onCheckedChange = null,
            enabled = state.isConfirmEnabled,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error),
        )
        Text(
            text = stringResource(R.string.delete_account_confirm),
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.isConfirmEnabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
            },
        )
    }
}

private fun createPreviewState(
    isOnline: Boolean = true,
    hasConfirmed: Boolean = true,
    hasFailed: Boolean = false,
): DeleteAccountUiState = DeleteAccountUiState(
    accountProvider = AccountProvider.GOOGLE,
    isOnline = isOnline,
    hasConfirmed = hasConfirmed,
    isDeleting = false,
    hasFailed = hasFailed,
)

@DevicePreviews
@Composable
private fun DeleteAccountPreview() {
    BlitzSplitTheme {
        DeleteAccountContent(
            state = createPreviewState(),
            isDialog = false,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun DeleteAccountOfflinePreview() {
    BlitzSplitTheme {
        DeleteAccountContent(
            state = createPreviewState(
                isOnline = false,
                hasConfirmed = false,
            ),
            isDialog = false,
            onEvent = {},
        )
    }
}

@Preview(
    name = "Tablet dialog",
    device = "spec:width=1280dp,height=800dp,orientation=landscape",
)
@Composable
private fun DeleteAccountDialogPreview() {
    BlitzSplitTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
            contentAlignment = Alignment.Center,
        ) {
            DeleteAccountContent(
                state = createPreviewState(hasFailed = true),
                isDialog = true,
                onEvent = {},
            )
        }
    }
}
