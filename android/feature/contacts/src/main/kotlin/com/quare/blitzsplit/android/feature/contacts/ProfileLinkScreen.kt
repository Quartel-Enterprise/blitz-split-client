package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.contacts.presentation.model.ProfileLinkStage
import com.quare.blitzsplit.feature.contacts.presentation.model.ProfileLinkUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ProfileLinkUiState
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ProfileLinkViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val columnMaxWidth = 560.dp
private val avatarSize = 112.dp

/*
 * Why: a profile link opened, blitzsplit.com/u/<code>, while it asks the server or when it has no contact to open:
 * a contact's link and the person's own give way to their profile at once. One centered column at every size and
 * posture, since it holds a few lines at most.
 */
@Composable
fun ProfileLinkScreen(
    route: Route.ProfileLink,
    viewModel: ProfileLinkViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileLinkContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileLinkContent(
    state: ProfileLinkUiState,
    onEvent: (ProfileLinkUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { onEvent(ProfileLinkUiEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(DesignR.string.designsystem_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            contentAlignment = Alignment.Center,
        ) {
            if (state.stage == ProfileLinkStage.LOADING) {
                CircularProgressIndicator()
                return@Box
            }
            Column(
                modifier = Modifier
                    .widthIn(max = columnMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = 24.dp,
                        vertical = 32.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (state.stage) {
                    ProfileLinkStage.STRANGER -> StrangerProfileContent(
                        state = state,
                        onEvent = onEvent,
                    )

                    ProfileLinkStage.UNREACHABLE -> LinkMessage(
                        icon = Icons.Outlined.CloudOff,
                        title = stringResource(R.string.profile_link_unreachable_title),
                        body = stringResource(R.string.profile_link_unreachable_body),
                    ) {
                        Button(onClick = { onEvent(ProfileLinkUiEvent.OnRetryClicked) }) {
                            Text(stringResource(R.string.profile_link_retry))
                        }
                    }

                    ProfileLinkStage.NOT_FOUND -> LinkMessage(
                        icon = Icons.Outlined.LinkOff,
                        title = stringResource(R.string.profile_link_not_found_title),
                        body = stringResource(R.string.profile_link_not_found_body),
                    )

                    ProfileLinkStage.LOADING -> Unit
                }
            }
        }
    }
}

@Composable
private fun StrangerProfileContent(
    state: ProfileLinkUiState,
    onEvent: (ProfileLinkUiEvent) -> Unit,
) {
    val name = state.name.orEmpty()
    val color = state.avatarColor ?: return
    AvatarBadge(
        initial = state.initials.orEmpty(),
        color = color,
        photoUrl = state.photoUrl,
        size = avatarSize,
    )
    Text(
        text = name,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Text(
        text = stringResource(R.string.profile_link_no_groups, name),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    if (state.canInvite) {
        FilledTonalButton(
            onClick = { onEvent(ProfileLinkUiEvent.OnInviteClicked) },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Text(
                text = stringResource(R.string.profile_link_invite),
                modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
            )
        }
    }
}

@Composable
private fun LinkMessage(
    icon: ImageVector,
    title: String,
    body: String,
    action: @Composable () -> Unit = {},
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = title,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    action()
}
