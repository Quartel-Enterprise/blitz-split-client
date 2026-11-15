package com.quare.blitzsplit.android.feature.releasenotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.EmptyStateColumn
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion
import com.quare.blitzsplit.core.releasenotes.domain.model.ReleaseDate
import com.quare.blitzsplit.core.releasenotes.domain.model.ReleaseNotes
import com.quare.blitzsplit.feature.releasenotes.presentation.model.ReleaseNotesUiEvent
import com.quare.blitzsplit.feature.releasenotes.presentation.model.ReleaseNotesUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val textColumnMaxWidth = 640.dp
private val contentPadding = PaddingValues(
    start = 16.dp,
    end = 16.dp,
    top = 8.dp,
    bottom = 32.dp,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseNotesContent(
    state: ReleaseNotesUiState,
    onEvent: (ReleaseNotesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.release_notes_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ReleaseNotesUiEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(DesignR.string.designsystem_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        // Why: one column of cards at every size, which only centers in a wider window, as the legal text does.
        AdaptiveScreen(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.showsLoadFailed -> LoadFailedColumn(onRetry = { onEvent(ReleaseNotesUiEvent.OnRetryClicked) })

                state.showsSkeleton -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(contentPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    ReleaseNotesSkeleton(modifier = Modifier.textColumn())
                }

                else -> ReleaseList(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun ReleaseList(
    state: ReleaseNotesUiState,
    onEvent: (ReleaseNotesUiEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.showsBetaSwitch) {
            item(
                key = "beta",
                contentType = "beta",
            ) {
                ShowBetaRow(
                    isOn = state.showsBeta == true,
                    onToggle = { isOn -> onEvent(ReleaseNotesUiEvent.OnShowBetaToggled(isOn)) },
                    modifier = Modifier.textColumn(),
                )
            }
        }
        if (state.showsEmpty) {
            item(
                key = "empty",
                contentType = "empty",
            ) {
                val textRes = if (state.hasOnlyBetaReleases) {
                    R.string.release_notes_empty_only_betas
                } else {
                    R.string.release_notes_empty
                }
                Text(
                    text = stringResource(textRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .textColumn()
                        .padding(
                            horizontal = 4.dp,
                            vertical = 16.dp,
                        ),
                )
            }
        }
        items(
            items = state.visibleReleases,
            key = { release -> release.version.fullName },
            contentType = { "release" },
        ) { release ->
            ReleaseNotesCard(
                release = release,
                modifier = Modifier
                    .textColumn()
                    .animateItem(),
            )
        }
    }
}

// Why: "Show beta versions", the whole row a switch for TalkBack and for the tap.
@Composable
private fun ShowBetaRow(
    isOn: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .toggleable(
                value = isOn,
                role = Role.Switch,
                onValueChange = onToggle,
            ).heightIn(min = 56.dp)
            .padding(
                horizontal = 4.dp,
                vertical = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.release_notes_show_beta),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = isOn,
            onCheckedChange = null,
        )
    }
}

@Composable
private fun LoadFailedColumn(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        EmptyStateColumn(
            icon = Icons.Outlined.CloudOff,
            title = stringResource(R.string.release_notes_load_failed_title),
            body = stringResource(R.string.release_notes_load_failed_body),
            action = stringResource(R.string.release_notes_retry),
            onAction = onRetry,
            modifier = Modifier.textColumn(),
        )
    }
}

private fun Modifier.textColumn(): Modifier = widthIn(max = textColumnMaxWidth).fillMaxWidth()

private fun createSampleReleases(): List<ReleaseNotes> = listOf(
    ReleaseNotes(
        version = AppVersion(
            major = 3,
            minor = 2,
            patch = 0,
            beta = 2,
        ),
        date = ReleaseDate(
            year = 2026,
            month = 11,
            day = 14,
        ),
        isInstalled = true,
        notes = listOf(
            "Release notes: see what each version changed, right from your profile.",
            "Home shows the people you split with the most as rows when there is room.",
        ),
    ),
    ReleaseNotes(
        version = AppVersion(
            major = 3,
            minor = 1,
            patch = 0,
            beta = null,
        ),
        date = ReleaseDate(
            year = 2026,
            month = 10,
            day = 28,
        ),
        isInstalled = false,
        notes = listOf(
            "Scan a receipt and split it item by item.",
            "Groups in bitcoin, with amounts in sats if you prefer.",
            "Fixed the keyboard covering the amount on small phones.",
        ),
    ),
)

@DevicePreviews
@Composable
private fun ReleaseNotesPreview() {
    BlitzSplitTheme {
        ReleaseNotesContent(
            state = ReleaseNotesUiState(
                releases = createSampleReleases(),
                showsBeta = true,
                hasLoadFailed = false,
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ReleaseNotesSkeletonPreview() {
    BlitzSplitTheme {
        ReleaseNotesContent(
            state = ReleaseNotesUiState(
                releases = null,
                showsBeta = null,
                hasLoadFailed = false,
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ReleaseNotesLoadFailedPreview() {
    BlitzSplitTheme {
        ReleaseNotesContent(
            state = ReleaseNotesUiState(
                releases = null,
                showsBeta = false,
                hasLoadFailed = true,
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ReleaseNotesEmptyPreview() {
    BlitzSplitTheme {
        ReleaseNotesContent(
            state = ReleaseNotesUiState(
                releases = emptyList(),
                showsBeta = false,
                hasLoadFailed = false,
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ReleaseNotesOnlyBetasPreview() {
    BlitzSplitTheme {
        ReleaseNotesContent(
            state = ReleaseNotesUiState(
                releases = createSampleReleases().filter { release -> release.version.isBeta },
                showsBeta = false,
                hasLoadFailed = false,
            ),
            onEvent = {},
        )
    }
}
