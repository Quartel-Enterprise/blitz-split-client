package com.quare.blitzsplit.android.feature.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.FlatListItem
import com.quare.blitzsplit.android.designsystem.component.OfflineNoticeBanner
import com.quare.blitzsplit.android.designsystem.component.TappableCard
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.LocalFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.TwoHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.tabletopFoldLayoutOf
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.android.designsystem.theme.pendingColor
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileSyncStatus
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import software.quare.loadable.Loadable
import com.quare.blitzsplit.android.designsystem.R as DesignR

// Why: the gap of flow 4's book, centered on the hinge: its 24dp band and 24dp on each side.
private val bookGap = 72.dp
private val singleColumnMaxWidth = 592.dp
private val phoneLandscapeStartWidth = 330.dp
private val tabletStartWidth = 380.dp
private val tabletMaxWidth = 1016.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    state: ProfileUiState,
    appVersion: String,
    onEvent: (ProfileUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ProfileUiEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(DesignR.string.designsystem_back),
                        )
                    }
                },
                actions = {
                    // Why: only an account has a link; the system share has Copy, so Android needs no menu.
                    if (state.shareLink != null) {
                        IconButton(onClick = { onEvent(ProfileUiEvent.OnShareProfileClicked) }) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.profile_share),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val isBook = rememberFoldLayout().bookSplit != null
        AdaptiveScreen(modifier = Modifier.fillMaxSize().padding(padding)) { layout ->
            when {
                isBook -> ProfileBookLayout(
                    state = state,
                    appVersion = appVersion,
                    onEvent = onEvent,
                )

                layout.usesTwoColumns -> ProfileTwoColumnLayout(
                    state = state,
                    appVersion = appVersion,
                    onEvent = onEvent,
                    isTablet = layout.isTablet,
                )

                else -> ProfileSingleColumn(
                    state = state,
                    appVersion = appVersion,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun ProfileSingleColumn(
    state: ProfileUiState,
    appVersion: String,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = singleColumnMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileIdentityContent(
                state = state,
                onEvent = onEvent,
            )
            ProfileKeysContent(
                state = state,
                onEvent = onEvent,
            )
            AboutSection(
                state = state,
                appVersion = appVersion,
                onEvent = onEvent,
            )
            ProfileDeleteAccountContent(
                state = state,
                onEvent = onEvent,
            )
            LegalFooter(
                appVersion = appVersion,
                onEvent = onEvent,
            )
        }
    }
}

/*
 * Why: lying down there is little height, so who you are, your account with signing out under it and the privacy rows
 * go left, the keys, preferences, deleting the account and the legal footer right, each column scrolling on its own: a 330dp column on a phone, 380dp in a tablet's
 * centered container.
 */
@Composable
private fun ProfileTwoColumnLayout(
    state: ProfileUiState,
    appVersion: String,
    onEvent: (ProfileUiEvent) -> Unit,
    isTablet: Boolean,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = if (isTablet) tabletMaxWidth else Dp.Infinity)
                .fillMaxSize()
                .padding(horizontal = if (isTablet) 48.dp else 24.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isTablet) 48.dp else 32.dp),
        ) {
            ProfileStartColumn(
                state = state,
                appVersion = appVersion,
                onEvent = onEvent,
                modifier = Modifier.width(if (isTablet) tabletStartWidth else phoneLandscapeStartWidth),
            )
            ProfileEndColumn(
                state = state,
                appVersion = appVersion,
                onEvent = onEvent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// Why: a book (and a tabletop lying down) splits on the hinge, the columns of lying down a half each.
@Composable
private fun ProfileBookLayout(
    state: ProfileUiState,
    appVersion: String,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    TwoHalvesLayout(
        start = {
            ProfileStartColumn(
                state = state,
                appVersion = appVersion,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 32.dp),
            )
        },
        end = {
            ProfileEndColumn(
                state = state,
                appVersion = appVersion,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 32.dp),
            )
        },
        modifier = Modifier.fillMaxSize(),
        gap = bookGap,
    )
}

@Composable
private fun ProfileStartColumn(
    state: ProfileUiState,
    appVersion: String,
    onEvent: (ProfileUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProfileIdentityContent(
            state = state,
            onEvent = onEvent,
        )
        AboutSection(
            state = state,
            appVersion = appVersion,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun ProfileEndColumn(
    state: ProfileUiState,
    appVersion: String,
    onEvent: (ProfileUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProfileKeysContent(
            state = state,
            onEvent = onEvent,
        )
        ProfileDeleteAccountContent(
            state = state,
            onEvent = onEvent,
        )
        // Why: the profile ends here, under deleting the account, as in one column (beta feedback 18).
        LegalFooter(
            appVersion = appVersion,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun ProfileIdentityContent(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    if (!state.isLoaded) {
        ProfileIdentitySkeleton()
        SignOutSkeleton()
        return
    }
    ProfileHeader(
        state = state,
        onEvent = onEvent,
    )
    AccountSection(
        state = state,
        onEvent = onEvent,
    )
    // Why: right under the account it leaves, never at the end of a long column (beta feedback 16).
    SignOutButton(
        state = state,
        onEvent = onEvent,
    )
}

@Composable
private fun ProfileKeysContent(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    PaymentKeysSection(
        state = state,
        onEvent = onEvent,
    )
    PreferencesSection(
        state = state,
        onEvent = onEvent,
    )
}

// Why: the last thing on the profile, apart from signing out.
@Composable
private fun ProfileDeleteAccountContent(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    if (state.isLoaded && state.showsDeleteAccount) DeleteAccountButton(onEvent = onEvent)
}

@Composable
private fun ProfileHeader(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.showsOfflineNotice) {
            OfflineNoticeBanner(
                title = stringResource(R.string.profile_offline_title),
                message = stringResource(R.string.profile_offline_body),
                isCompact = true,
            )
        }
        IdentityCard(
            state = state,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun IdentityCard(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    TappableCard(
        onClick = { onEvent(ProfileUiEvent.OnEditClicked) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Why: the avatar opens the photo big, as Bible Planner's; the rest of the card edits the profile.
            AvatarBadge(
                initial = state.avatarInitial,
                color = state.avatarColor,
                size = 72.dp,
                photoUrl = state.photoUrl,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        onClickLabel = stringResource(R.string.profile_photo_open),
                        role = Role.Button,
                        onClick = { onEvent(ProfileUiEvent.OnAvatarClicked) },
                    ),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        if (state.showsProfilePending) R.string.profile_pending else R.string.profile_edit_subtitle,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.showsProfilePending) {
                        pendingColor()
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.profile_edit),
            )
        }
    }
}

@Composable
internal fun Section(
    @StringRes title: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(title),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(content = content)
        }
    }
}

@Composable
internal fun SectionRow(
    icon: ImageVector,
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    // Why: the BCP-47 tag of a supporting text in another language than the app's, a language's own name.
    supportingLanguageTag: String? = null,
    isSupportingLoading: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    FlatListItem(
        headlineContent = { Text(headline) },
        modifier = modifier,
        supportingContent = if (isSupportingLoading) {
            { ProfileValueSkeleton() }
        } else {
            supporting?.let {
                {
                    Text(
                        text = it,
                        style = LocalTextStyle.current.copy(
                            localeList = supportingLanguageTag?.let { tag ->
                                LocaleList(tag)
                            },
                        ),
                    )
                }
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        trailingContent = trailing,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

@DevicePreviews
@Composable
private fun ProfileConnectedPreview() {
    BlitzSplitTheme {
        ProfileContent(
            state = createPreviewProfileState(),
            appVersion = "1.0.0",
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ProfileLoadingPreview() {
    BlitzSplitTheme {
        ProfileContent(
            state = createPreviewProfileState().copy(
                isLoaded = false,
                paymentKeys = Loadable.Loading,
                activeNotificationCount = Loadable.Loading,
            ),
            appVersion = "1.0.0",
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ProfileAnonymousOfflinePreview() {
    BlitzSplitTheme {
        ProfileContent(
            state = createPreviewProfileState(
                accountProvider = null,
                isOnline = false,
                syncStatus = ProfileSyncStatus.WAITING,
            ),
            appVersion = "1.0.0",
            onEvent = {},
        )
    }
}

// Why: a preview's window reports no fold, so the tabletop's hinge is handed in.
@Preview(
    name = "Tabletop",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun ProfileTabletopPreview() {
    CompositionLocalProvider(LocalFoldLayout provides tabletopFoldLayoutOf(841.dp, 673.dp)) {
        BlitzSplitTheme {
            ProfileContent(
                state = createPreviewProfileState(),
                appVersion = "1.0.0",
                onEvent = {},
            )
        }
    }
}
