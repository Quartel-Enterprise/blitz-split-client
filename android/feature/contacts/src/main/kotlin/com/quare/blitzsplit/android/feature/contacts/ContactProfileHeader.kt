package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.ProfileFlightPieces
import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiState

private val barAvatarSize = 32.dp

/*
 * Why: the face and the name in the bar, coming in as the header goes up under it (beta feedback of 2026-11-14), so
 * whose profile it is stays in sight. TalkBack reads the header's, never both.
 */
@Composable
internal fun BarTitle(
    state: ContactProfileUiState,
    contact: Contact,
    fold: () -> Float,
) {
    val slide = with(LocalDensity.current) { 8.dp.toPx() }
    Row(
        modifier = Modifier
            .graphicsLayer {
                val shown = ((fold() - 0.5f) * 2f).coerceIn(0f, 1f)
                alpha = shown
                translationY = (1f - shown) * slide
            }.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBadge(
            initial = state.initials,
            color = contact.color,
            photoUrl = contact.photoUrl,
            size = barAvatarSize,
        )
        Text(
            text = contact.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/*
 * Why: the face shrinks and fades towards the bar as the header goes up, the name and the line under it fading with
 * it, while the bar's own face and name come in.
 */
@Composable
internal fun ProfileHeader(
    state: ContactProfileUiState,
    contact: Contact,
    avatarSize: Dp,
    flight: ProfileFlightPieces,
    fold: () -> Float,
    onEvent: (ContactProfileUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Why: the face opens big, as the profile's avatar and the group's photo do.
        AvatarBadge(
            initial = state.initials,
            color = contact.color,
            photoUrl = contact.photoUrl,
            size = avatarSize,
            modifier = flight.face
                .graphicsLayer {
                    val progress = fold()
                    val scale = 1f - progress * 0.5f
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - progress
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }.clip(CircleShape)
                .clickable(
                    onClickLabel = stringResource(R.string.contacts_profile_photo_open),
                    role = Role.Button,
                    onClick = { onEvent(ContactProfileUiEvent.OnPhotoClicked) },
                ),
        )
        Column(
            modifier = Modifier.graphicsLayer { alpha = (1f - fold() * 1.5f).coerceIn(0f, 1f) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = contact.name,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .then(flight.name)
                    .semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (contact.isGuest) {
                    stringResource(R.string.contacts_profile_meta_guest)
                } else {
                    stringResource(
                        R.string.contacts_profile_meta_user,
                        pluralStringResource(
                            R.plurals.contacts_groups_in_common,
                            contact.groups.size,
                            contact.groups.size,
                        ),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
