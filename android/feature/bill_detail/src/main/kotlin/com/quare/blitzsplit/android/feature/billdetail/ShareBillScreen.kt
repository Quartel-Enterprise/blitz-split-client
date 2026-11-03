package com.quare.blitzsplit.android.feature.billdetail

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.InfoNoteBanner
import com.quare.blitzsplit.android.designsystem.component.OfflineChip
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerCardSkeleton
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.android.designsystem.format.nameListOf
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.FormPresentation
import com.quare.blitzsplit.android.designsystem.layout.TopBottomHalvesLayout
import com.quare.blitzsplit.android.platform.copyLink
import com.quare.blitzsplit.android.platform.shareText
import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillError
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillUiState
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.ShareBillViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val dialogWidth = 560.dp
private const val DISABLED_ALPHA = 0.38f
private const val DISABLED_BORDER_ALPHA = 0.12f

/*
 * Why: "Compartilhar conta" (7.3) over Conta, opened as Convidar of Contatos opens: a bottom sheet on a phone, a 560dp
 * dialog elsewhere (in the half after the hinge on a book), and on a tabletop the link above the hinge with who may
 * open it and the buttons below it.
 */
@Composable
fun ShareBillScreen(
    route: Route.ShareBill,
    presentation: FormPresentation,
    viewModel: ShareBillViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                is ShareBillUiAction.CopyLink -> {
                    context.copyLink(action.link)
                    // Why: Android 13 and later confirm what was copied themselves.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        showReplacingSnackbar(snackbarHostState, resources.getString(R.string.share_bill_copied))
                    }
                }

                is ShareBillUiAction.SendLink -> context.shareText(
                    resources.getString(R.string.share_bill_message, action.description, action.link),
                )

                is ShareBillUiAction.ShowError -> showReplacingSnackbar(
                    snackbarHostState = snackbarHostState,
                    message = resources.getString(
                        when (action.error) {
                            ShareBillError.UPDATE_FAILED -> R.string.share_bill_update_error
                            ShareBillError.DISABLE_FAILED -> R.string.share_bill_disable_error
                        },
                    ),
                )
            }
        }
    }
    ShareBillContent(
        state = state,
        presentation = presentation,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
}

@Composable
internal fun ShareBillContent(
    state: ShareBillUiState,
    presentation: FormPresentation,
    snackbarHostState: SnackbarHostState,
    onEvent: (ShareBillUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.share_bill_title)
    when (presentation) {
        FormPresentation.BottomSheet -> Box(modifier = modifier.semantics { paneTitle = title }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 24.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ShareHeader(state, onEvent)
                LinkSection(state, onEvent)
                ShareSettingsSection(state, onEvent)
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        FormPresentation.Dialog -> Box(
            modifier = modifier.semantics { paneTitle = title },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = dialogWidth)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ShareHeader(state, onEvent)
                    LinkSection(state, onEvent)
                    ShareSettingsSection(state, onEvent)
                }
            }
            SnackbarHost(snackbarHostState)
        }

        FormPresentation.TabletopHalves -> Surface(
            modifier = modifier
                .fillMaxSize()
                .semantics { paneTitle = title },
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box {
                TopBottomHalvesLayout(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                    top = {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ShareHeader(state, onEvent)
                            LinkSection(state, onEvent)
                        }
                    },
                    bottom = {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            ShareSettingsSection(state, onEvent)
                        }
                    },
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                )
            }
        }
    }
}

// Why: "Compartilhar conta", "Jantar no Ostradamus · R$ 642,00" under it, and ✕.
@Composable
private fun ShareHeader(
    state: ShareBillUiState,
    onEvent: (ShareBillUiEvent) -> Unit,
) {
    val money = rememberMoneyFormatter(state.currency)
    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.share_bill_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            if (state.description.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.share_bill_subtitle,
                        state.description,
                        money.toText(state.totalMinor),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = { onEvent(ShareBillUiEvent.OnCloseClicked) }) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.share_bill_close),
            )
        }
    }
}

/*
 * Why: the link in a field with "Copiar", shimmering while it is read or made, "Tentar de novo" once that failed, and
 * nothing while it was never read (offline from the start), which the offline notice explains.
 */
@Composable
private fun LinkSection(
    state: ShareBillUiState,
    onEvent: (ShareBillUiEvent) -> Unit,
) {
    val link = state.displayLink
    Box(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        when {
            state.isLoading -> ShimmerBox {
                ShimmerCardSkeleton(
                    height = 56.dp,
                    shape = MaterialTheme.shapes.large,
                )
            }

            state.hasFailed -> LinkField {
                Text(
                    text = stringResource(R.string.share_bill_failed),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(
                    onClick = { onEvent(ShareBillUiEvent.OnRetryClicked) },
                    enabled = state.isOnline,
                ) {
                    Text(stringResource(R.string.share_bill_retry))
                }
            }

            link != null -> LinkField {
                Text(
                    text = link,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(
                    onClick = { onEvent(ShareBillUiEvent.OnCopyClicked) },
                    enabled = state.canSendLink,
                ) {
                    Icon(
                        imageVector = if (state.isLinkCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Text(
                        text = stringResource(
                            if (state.isLinkCopied) R.string.share_bill_copied else R.string.share_bill_copy,
                        ),
                        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkField(content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 4.dp,
                top = 4.dp,
                bottom = 4.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

// Why: "Quem pode abrir", the note on what the link shows, the offline notice and the buttons.
@Composable
private fun ShareSettingsSection(
    state: ShareBillUiState,
    onEvent: (ShareBillUiEvent) -> Unit,
) {
    VisibilitySection(state, onEvent)
    InfoNoteBanner(text = stringResource(R.string.share_bill_note))
    if (state.showsOfflineNotice) {
        OfflineChip(text = stringResource(R.string.share_bill_offline))
    }
    ShareButtonsRow(state, onEvent)
}

@Composable
private fun VisibilitySection(
    state: ShareBillUiState,
    onEvent: (ShareBillUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.share_bill_who_can_open),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.selectableGroup()) {
            VisibilityOption(
                title = stringResource(R.string.share_bill_members),
                detail = if (state.memberNames.isEmpty()) {
                    stringResource(R.string.share_bill_members_anyone_in_group)
                } else {
                    pluralStringResource(
                        R.plurals.share_bill_members_named,
                        state.memberNames.size,
                        nameListOf(state.memberNames),
                    )
                },
                isSelected = state.visibility == BillLinkVisibility.MEMBERS,
                isEnabled = state.canChangeLink,
                onClick = { onEvent(ShareBillUiEvent.OnVisibilityPicked(BillLinkVisibility.MEMBERS)) },
            )
            VisibilityOption(
                title = stringResource(R.string.share_bill_anyone),
                detail = stringResource(R.string.share_bill_anyone_detail),
                isSelected = state.visibility == BillLinkVisibility.ANYONE,
                isEnabled = state.canChangeLink,
                onClick = { onEvent(ShareBillUiEvent.OnVisibilityPicked(BillLinkVisibility.ANYONE)) },
            )
        }
    }
}

@Composable
private fun VisibilityOption(
    title: String,
    detail: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    val alpha = if (isEnabled) 1f else DISABLED_ALPHA
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .selectable(
                selected = isSelected,
                enabled = isEnabled,
                role = Role.RadioButton,
                onClick = onClick,
            ).padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            enabled = isEnabled,
            modifier = Modifier.padding(12.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    top = 10.dp,
                    end = 8.dp,
                    bottom = 8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
    }
}

@Composable
private fun ShareButtonsRow(
    state: ShareBillUiState,
    onEvent: (ShareBillUiEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val error = MaterialTheme.colorScheme.error
        OutlinedButton(
            onClick = { onEvent(ShareBillUiEvent.OnDisableClicked) },
            enabled = state.canChangeLink,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = error),
            border = BorderStroke(
                width = 1.dp,
                color = if (state.canChangeLink) {
                    error
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_BORDER_ALPHA)
                },
            ),
        ) {
            Icon(
                imageVector = Icons.Outlined.LinkOff,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Text(
                text = stringResource(R.string.share_bill_disable),
                modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
            )
        }
        Button(
            onClick = { onEvent(ShareBillUiEvent.OnSendClicked) },
            enabled = state.canSendLink,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Text(
                text = stringResource(R.string.share_bill_send),
                modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
            )
        }
    }
}
