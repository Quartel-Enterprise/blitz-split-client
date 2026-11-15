package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.DialogSystemBarsEffect
import com.quare.blitzsplit.android.designsystem.component.PhotoViewerContent
import com.quare.blitzsplit.android.designsystem.component.photoViewerPhoto
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FoldDialog
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: the person's face opened big, in the viewer the profile's and the group's photo share, the whole window dimmed
 * behind it (a book's end half, as any dialog), with no shortcuts: the photo is theirs to change, not this user's.
 */
@Composable
internal fun ContactPhotoDialog(
    contact: Contact,
    initials: String,
    onDismiss: () -> Unit,
) {
    FoldDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        // Why: light icons on the dimmed backdrop, which the dialog's window would otherwise draw dark.
        DialogSystemBarsEffect(isLight = false)
        ContactPhotoContent(
            contact = contact,
            initials = initials,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ContactPhotoContent(
    contact: Contact,
    initials: String,
    onDismiss: () -> Unit,
) {
    PhotoViewerContent(
        isLoaded = true,
        closeDescription = stringResource(R.string.contacts_close),
        onDismiss = onDismiss,
    ) { avatarSize ->
        AvatarBadge(
            initial = initials,
            color = contact.color,
            photoUrl = contact.photoUrl,
            size = avatarSize,
            modifier = Modifier.photoViewerPhoto(
                shape = CircleShape,
                description = contact.name,
            ),
        )
    }
}

@DevicePreviews
@Composable
private fun ContactPhotoContentPreview() {
    BlitzSplitTheme {
        ContactPhotoContent(
            contact = Contact(
                key = "user:carla",
                name = "Carla Souza",
                color = AvatarColor.entries.first(),
                photoUrl = null,
                userId = "carla",
                groups = emptyList(),
                nets = emptyList(),
                isHidden = false,
            ),
            initials = "CS",
            onDismiss = {},
        )
    }
}
