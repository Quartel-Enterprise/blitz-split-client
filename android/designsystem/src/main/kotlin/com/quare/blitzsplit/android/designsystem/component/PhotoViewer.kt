package com.quare.blitzsplit.android.designsystem.component

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider

// Why: Bible Planner's viewer, which Perfil's avatar opens as it does there (beta feedback 16), and a group's too.
private val compactAvatarSize = 200.dp
private val expandedAvatarSize = 260.dp
private val expandedBreakpoint = 600.dp
private val shortcutSize = 58.dp
private val shortcutLabelWidth = 76.dp
val PhotoViewerIconSize = 26.dp
private val closeButtonSize = 44.dp
private val backdropBlurRadius = 18.dp
private val backdropColor = Color(0xFF08080E).copy(alpha = 0.56f)
private val closeButtonBackground = Color.Black.copy(alpha = 0.32f)
private val neutralShortcutBackground = Color.White.copy(alpha = 0.16f)
private val destructiveShortcutBackground = Color(0xFFFF7878).copy(alpha = 0.22f)
private val destructiveContent = Color(0xFFFF9A9A)

/*
 * Why: the photo big on a dimmed, blurred backdrop, with the shortcuts that change it under it, for the profile's and
 * the group's alike. A tap outside them, ✕ and back close it. `content` draws the photo and its shortcuts, given the
 * size the photo takes: 260 dp where both sides have 600, 200 dp otherwise.
 */
@Composable
fun PhotoViewerContent(
    isLoaded: Boolean,
    closeDescription: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable ColumnScope.(avatarSize: Dp) -> Unit,
) {
    DialogWindowBlurEffect(radius = backdropBlurRadius)
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
        // Why: only what was read is drawn, never "?" on the default color.
        if (isLoaded) {
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .pointerInput(Unit) {}
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                content(avatarSize)
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
                contentDescription = closeDescription,
                modifier = Modifier.size(PhotoViewerIconSize),
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

// Why: the spinner over a photo being prepared, in the photo's own shape.
@Composable
fun PhotoViewerPreparingOverlay(
    size: Dp,
    shape: Shape,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

// Why: "Câmera", "Galeria" and, with a photo, "Remover", the shortcuts both viewers share; each adds its own first.
@Composable
fun PhotoViewerShortcutsRow(
    cameraLabel: String,
    cameraDescription: String,
    galleryLabel: String,
    galleryDescription: String,
    removeLabel: String,
    removeDescription: String,
    onTakePhotoClick: (() -> Unit)?,
    onChoosePhotoClick: () -> Unit,
    onRemoveClick: (() -> Unit)?,
    leading: @Composable () -> Unit = {},
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        leading()
        if (onTakePhotoClick != null) {
            PhotoShortcutButton(
                icon = { ShortcutIcon(Icons.Outlined.PhotoCamera) },
                label = cameraLabel,
                accessibilityLabel = cameraDescription,
                onClick = onTakePhotoClick,
            )
        }
        PhotoShortcutButton(
            icon = { ShortcutIcon(Icons.Outlined.PhotoLibrary) },
            label = galleryLabel,
            accessibilityLabel = galleryDescription,
            onClick = onChoosePhotoClick,
        )
        if (onRemoveClick != null) {
            PhotoShortcutButton(
                icon = { ShortcutIcon(Icons.Outlined.Delete, tint = destructiveContent) },
                label = removeLabel,
                accessibilityLabel = removeDescription,
                onClick = onRemoveClick,
                background = destructiveShortcutBackground,
                content = destructiveContent,
            )
        }
    }
}

// Why: the line under the photo when the photo picked couldn't be used, in both viewers.
@Composable
fun PhotoViewerErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = destructiveContent,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun ShortcutIcon(
    icon: ImageVector,
    tint: Color = Color.White,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(PhotoViewerIconSize),
        tint = tint,
    )
}

@Composable
fun PhotoShortcutButton(
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
