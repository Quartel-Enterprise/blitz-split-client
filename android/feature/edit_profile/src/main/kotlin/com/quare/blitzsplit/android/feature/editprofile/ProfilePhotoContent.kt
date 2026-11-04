package com.quare.blitzsplit.android.feature.editprofile

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.labelRes
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.editprofile.presentation.model.ProfilePhotoUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.model.ProfilePhotoUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

// Why: Bible Planner's viewer, which Perfil's avatar opens as it does there (beta feedback 16).
private val compactAvatarSize = 200.dp
private val expandedAvatarSize = 260.dp
private val expandedBreakpoint = 600.dp
private val shortcutSize = 58.dp
private val shortcutLabelWidth = 76.dp
private val shortcutIconSize = 26.dp
private val closeButtonSize = 44.dp
private val backdropBlurRadius = 18.dp
private val backdropColor = Color(0xFF08080E).copy(alpha = 0.56f)
private val closeButtonBackground = Color.Black.copy(alpha = 0.32f)
private val neutralShortcutBackground = Color.White.copy(alpha = 0.16f)
private val destructiveShortcutBackground = Color(0xFFFF7878).copy(alpha = 0.22f)
private val destructiveShortcutContent = Color(0xFFFF9A9A)

/*
 * Why: the photo (or the initial on its color) big on a dimmed, blurred backdrop, with the shortcuts that change it
 * under it. A tap outside them, ✕ and back close it.
 */
@Composable
internal fun ProfilePhotoContent(
    state: ProfilePhotoUiState,
    onEvent: (ProfilePhotoUiEvent) -> Unit,
    onTakePhotoClick: (() -> Unit)?,
    onChoosePhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
) {
    DialogWindowBlurEffect(radius = backdropBlurRadius)
    val onDismiss = { onEvent(ProfilePhotoUiEvent.OnDismissed) }
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Why: the smaller side, so a phone lying down keeps the compact photo and its shortcuts fit.
        val avatarSize = if (minOf(maxWidth, maxHeight) >= expandedBreakpoint) expandedAvatarSize else compactAvatarSize
        // Why: the backdrop sits behind the photo, as FoldDialog's scrim, so only a tap outside it closes the viewer.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .pointerInput(onDismiss) { detectTapGestures { onDismiss() } },
        )
        // Why: only the saved profile is drawn, never "?" on the default color.
        if (state.isLoaded) {
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .pointerInput(Unit) {}
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                LargeAvatarImage(
                    state = state,
                    size = avatarSize,
                )
                if (state.showsPhotoError) {
                    Text(
                        text = stringResource(R.string.edit_profile_photo_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = destructiveShortcutContent,
                        textAlign = TextAlign.Center,
                    )
                }
                PhotoShortcutRow(
                    state = state,
                    onEvent = onEvent,
                    onTakePhotoClick = onTakePhotoClick,
                    onChoosePhotoClick = onChoosePhotoClick,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(14.dp)
                .size(closeButtonSize)
                .clip(CircleShape)
                .background(closeButtonBackground)
                .clickable(
                    role = Role.Button,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.profile_photo_close),
                modifier = Modifier.size(shortcutIconSize),
                tint = Color.White,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding(),
        ) {
            snackbarHost()
        }
    }
}

@Composable
private fun LargeAvatarImage(
    state: ProfilePhotoUiState,
    size: Dp,
) {
    val description = stringResource(R.string.profile_photo_title)
    Box(contentAlignment = Alignment.Center) {
        AvatarBadge(
            initial = state.avatarInitial,
            color = state.avatarColor,
            size = size,
            photoUrl = state.photoUrl,
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                ).semantics { contentDescription = description },
        )
        if (state.isPreparingPhoto) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
private fun PhotoShortcutRow(
    state: ProfilePhotoUiState,
    onEvent: (ProfilePhotoUiEvent) -> Unit,
    onTakePhotoClick: (() -> Unit)?,
    onChoosePhotoClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        state.accountPhotoProvider?.let { provider ->
            // Why: the account's logo on white, as Bible Planner's viewer and the sign in buttons draw it.
            PhotoShortcutButton(
                icon = {
                    if (provider == AccountProvider.GOOGLE) {
                        Image(
                            painter = painterResource(DesignR.drawable.ic_google_logo),
                            contentDescription = null,
                            modifier = Modifier.size(shortcutIconSize),
                        )
                    } else {
                        Icon(
                            painter = painterResource(DesignR.drawable.ic_apple_logo),
                            contentDescription = null,
                            modifier = Modifier.size(shortcutIconSize),
                            tint = Color.Black,
                        )
                    }
                },
                background = Color.White,
                label = stringResource(provider.labelRes),
                accessibilityLabel = stringResource(
                    R.string.edit_profile_use_account_photo,
                    stringResource(provider.labelRes),
                ),
                onClick = { onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected) },
            )
        }
        if (onTakePhotoClick != null) {
            PhotoShortcutButton(
                icon = { ShortcutIcon(Icons.Outlined.PhotoCamera) },
                label = stringResource(R.string.profile_photo_camera),
                accessibilityLabel = stringResource(R.string.edit_profile_take_photo),
                onClick = onTakePhotoClick,
            )
        }
        PhotoShortcutButton(
            icon = { ShortcutIcon(Icons.Outlined.PhotoLibrary) },
            label = stringResource(R.string.profile_photo_gallery),
            accessibilityLabel = stringResource(R.string.edit_profile_choose_photo),
            onClick = onChoosePhotoClick,
        )
        if (state.hasPhoto) {
            PhotoShortcutButton(
                icon = { ShortcutIcon(Icons.Outlined.Delete, tint = destructiveShortcutContent) },
                label = stringResource(R.string.profile_photo_remove),
                accessibilityLabel = stringResource(R.string.edit_profile_remove_photo),
                onClick = { onEvent(ProfilePhotoUiEvent.OnPhotoRemoved) },
                background = destructiveShortcutBackground,
                content = destructiveShortcutContent,
            )
        }
    }
}

@Composable
private fun ShortcutIcon(
    icon: ImageVector,
    tint: Color = Color.White,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(shortcutIconSize),
        tint = tint,
    )
}

@Composable
private fun PhotoShortcutButton(
    icon: @Composable () -> Unit,
    label: String,
    accessibilityLabel: String,
    onClick: () -> Unit,
    background: Color = neutralShortcutBackground,
    content: Color = Color.White,
) {
    Column(
        modifier = Modifier
            .width(shortcutLabelWidth)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                onClickLabel = accessibilityLabel,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(shortcutSize)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            textAlign = TextAlign.Center,
        )
    }
}

// Why: Android 12 and later blur what is behind the dialog's window, as Bible Planner's viewer does; older ones dim it.
@Composable
private fun DialogWindowBlurEffect(radius: Dp) {
    val view = LocalView.current
    val radiusPx = with(LocalDensity.current) { radius.roundToPx() }
    SideEffect {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@SideEffect
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.attributes = window.attributes.apply { blurBehindRadius = radiusPx }
    }
}

@DevicePreviews
@Composable
private fun ProfilePhotoContentPreview() {
    BlitzSplitTheme {
        ProfilePhotoContent(
            state = ProfilePhotoUiState(
                isLoaded = true,
                displayName = "Pierre Vieira",
                avatarColor = AvatarColor.BLUE,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                photoCrop = null,
                accountProvider = AccountProvider.GOOGLE,
                accountPhotoUrl = "https://example.com/photo.jpg",
            ),
            onEvent = {},
            onTakePhotoClick = {},
            onChoosePhotoClick = {},
        )
    }
}
