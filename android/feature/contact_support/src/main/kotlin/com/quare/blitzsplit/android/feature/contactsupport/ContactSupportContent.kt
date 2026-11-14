package com.quare.blitzsplit.android.feature.contactsupport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.DialogCard
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerLine
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.support.domain.model.SupportAccount
import com.quare.blitzsplit.core.support.domain.model.SupportDiagnostics
import com.quare.blitzsplit.core.support.domain.model.SupportPlatform
import com.quare.blitzsplit.feature.contactsupport.presentation.model.ContactSupportNotice
import com.quare.blitzsplit.feature.contactsupport.presentation.model.ContactSupportUiEvent
import com.quare.blitzsplit.feature.contactsupport.presentation.model.ContactSupportUiState
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded

private const val DIAGNOSTICS_SKELETON_ROWS = 4

@Composable
fun ContactSupportContent(
    state: ContactSupportUiState,
    // Why: a tablet and a book draw it as a dialog over the profile; a phone as a bottom sheet.
    isDialog: Boolean,
    onEvent: (ContactSupportUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isDialog) {
        DialogCard(
            isWide = false,
            spacing = 16.dp,
            snackbarHostState = remember { SnackbarHostState() },
            modifier = modifier,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { onEvent(ContactSupportUiEvent.OnDismissed) },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.contact_support_close),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(
                        weight = 1f,
                        fill = false,
                    ).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ContactSupportDetailsSection(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ContactSupportDetailsSection(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun ColumnScope.ContactSupportDetailsSection(
    state: ContactSupportUiState,
    onEvent: (ContactSupportUiEvent) -> Unit,
) {
    ContactSupportHeading()
    EmailCard(
        email = state.email,
        onEvent = onEvent,
    )
    DiagnosticsCard(diagnostics = state.diagnostics)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { onEvent(ContactSupportUiEvent.OnOpenEmailClicked) },
            enabled = state.canOpenEmail,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(
                text = stringResource(R.string.contact_support_open_email),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        OutlinedButton(
            onClick = { onEvent(ContactSupportUiEvent.OnCopyEmailClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.contact_support_copy_email)) }
        state.notice?.let { notice ->
            // Why: a screen reader hears what the tap did, since nothing else on the sheet changes.
            Text(
                text = stringResource(notice.textRes),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ContactSupportHeading() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                )
            }
        }
        Text(
            text = stringResource(R.string.contact_support_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.contact_support_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// Why: the address is there to read and to select, since the email may be written on another device.
@Composable
private fun EmailCard(
    email: String,
    onEvent: (ContactSupportUiEvent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 4.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.contact_support_to),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SelectionContainer {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            IconButton(onClick = { onEvent(ContactSupportUiEvent.OnCopyEmailClicked) }) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.contact_support_copy_email),
                )
            }
        }
    }
}

// Why: what the email carries shimmers until it was read, so the card never shows a value it would then change.
@Composable
private fun DiagnosticsCard(diagnostics: Loadable<SupportDiagnostics>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.contact_support_diagnostics),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (diagnostics) {
                    Loadable.Loading -> ShimmerBox {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            repeat(DIAGNOSTICS_SKELETON_ROWS) {
                                ShimmerLine(
                                    width = 220.dp,
                                    height = 14.dp,
                                )
                            }
                        }
                    }

                    is Loadable.Loaded -> DiagnosticsList(diagnostics.value)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsList(diagnostics: SupportDiagnostics) {
    DiagnosticsRow(
        label = stringResource(R.string.contact_support_app_version),
        value = diagnostics.appVersion,
    )
    DiagnosticsRow(
        label = stringResource(R.string.contact_support_platform),
        value = diagnostics.platform.label,
    )
    DiagnosticsRow(
        label = stringResource(R.string.contact_support_language),
        // Why: a pick of System alone would not say which language the app speaks.
        value = diagnostics.language.tag
            ?: "${stringResource(R.string.contact_support_language_system)} (${diagnostics.spokenLanguage})",
    )
    DiagnosticsRow(
        label = stringResource(R.string.contact_support_account),
        value = diagnostics.account.toLabel(),
    )
    diagnostics.profileLink?.let { link ->
        DiagnosticsRow(
            label = stringResource(R.string.contact_support_profile),
            value = link,
        )
    }
}

@Composable
private fun DiagnosticsRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}

private val ContactSupportNotice.textRes: Int
    get() = when (this) {
        ContactSupportNotice.EMAIL_COPIED -> R.string.contact_support_notice_copied
        ContactSupportNotice.NO_EMAIL_APP -> R.string.contact_support_notice_no_app
    }

// Why: the names of the platforms and of Google and Apple are the same in every language.
private val SupportPlatform.label: String
    get() = when (this) {
        SupportPlatform.ANDROID -> "Android"
        SupportPlatform.IOS -> "iOS"
        SupportPlatform.MACOS -> "macOS"
        SupportPlatform.WEB -> "Web"
    }

@Composable
private fun SupportAccount.toLabel(): String = when (this) {
    SupportAccount.LOCAL_ONLY -> stringResource(R.string.contact_support_account_local)
    SupportAccount.ANONYMOUS -> stringResource(R.string.contact_support_account_anonymous)
    SupportAccount.GOOGLE -> "Google"
    SupportAccount.APPLE -> "Apple"
}

private fun createPreviewState(
    diagnostics: Loadable<SupportDiagnostics> = SupportDiagnostics(
        appVersion = "3.0.0",
        platform = SupportPlatform.ANDROID,
        language = AppLanguage.PORTUGUESE,
        spokenLanguage = "pt",
        account = SupportAccount.GOOGLE,
        profileLink = "https://blitzsplit.com/u/k7m2p9",
    ).toLoaded(),
    notice: ContactSupportNotice? = null,
): ContactSupportUiState = ContactSupportUiState(
    email = "appborarachar@gmail.com",
    diagnostics = diagnostics,
    notice = notice,
)

@DevicePreviews
@Composable
private fun ContactSupportSheetPreview() {
    BlitzSplitTheme {
        Surface {
            ContactSupportContent(
                state = createPreviewState(notice = ContactSupportNotice.NO_EMAIL_APP),
                isDialog = false,
                onEvent = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun ContactSupportDialogLoadingPreview() {
    BlitzSplitTheme {
        ContactSupportContent(
            state = createPreviewState(diagnostics = Loadable.Loading),
            isDialog = true,
            onEvent = {},
        )
    }
}
