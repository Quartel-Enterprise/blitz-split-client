package com.quare.blitzsplit.android.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.FoldAlertDialog
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveSheet
import com.quare.blitzsplit.android.designsystem.layout.DialogPlacement
import com.quare.blitzsplit.android.designsystem.theme.blitzSplitColorScheme
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState

/*
 * Why: "Aparência" as Bible Planner draws it: a card per theme with a preview of the app in it, then "Cores dinâmicas"
 * where Material You exists, in the app's short sheet, as the profile's other dialogs keep off the hinge.
 */
@Composable
internal fun AppearanceSheet(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    AdaptiveSheet(
        title = stringResource(R.string.profile_appearance),
        onDismissRequest = { onEvent(ProfileUiEvent.OnDialogDismissed) },
    ) {
        AppearanceContent(
            state = state,
            onEvent = onEvent,
        )
    }
}

// Why: what "Cores dinâmicas" does, over Aparência; "Entendi" goes back to it.
@Composable
internal fun DynamicColorsInfoDialog(
    onDismiss: () -> Unit,
    placement: DialogPlacement,
) {
    FoldAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Wallpaper,
                contentDescription = null,
            )
        },
        title = stringResource(R.string.profile_dynamic_colors),
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.profile_dynamic_colors_info_body))
                Text(
                    text = stringResource(R.string.profile_dynamic_colors_info_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_dynamic_colors_info_ok)) }
        },
        placement = placement,
    )
}

@Composable
private fun AppearanceContent(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    // Why: the previews show the light and the dark scheme the app would draw, the wallpaper's while it is on.
    val lightScheme = blitzSplitColorScheme(
        isDark = false,
        usesDynamicColors = state.usesDynamicColors,
    )
    val darkScheme = blitzSplitColorScheme(
        isDark = true,
        usesDynamicColors = state.usesDynamicColors,
    )
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_appearance),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
        ) {
            ThemePreference.entries.forEach { theme ->
                ThemeOptionCard(
                    theme = theme,
                    isSelected = state.theme == theme,
                    previewColors = theme.toPreviewColors(
                        lightScheme = lightScheme,
                        darkScheme = darkScheme,
                    ),
                    onClick = { onEvent(ProfileUiEvent.OnThemeSelected(theme)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (state.showsDynamicColors) {
            DynamicColorsRow(
                isOn = state.usesDynamicColors,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun ThemeOptionCard(
    theme: ThemePreference,
    isSelected: Boolean,
    previewColors: ThemePreviewColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        selected = isSelected,
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) primary.copy(alpha = 0.12f) else Color.Transparent,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier.semantics { role = Role.RadioButton },
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                ThemePreviewComponent(
                    colors = previewColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isSelected) {
                    SelectedBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp),
                    )
                }
            }
            ThemeOptionLabel(
                theme = theme,
                isSelected = isSelected,
            )
        }
    }
}

@Composable
private fun ThemeOptionLabel(
    theme: ThemePreference,
    isSelected: Boolean,
) {
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = 5.dp,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = theme.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(theme.labelRes),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SelectedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(13.dp),
        )
    }
}

// Why: a sketch of a screen in the theme: a title, two lines of text and a button in its primary color.
@Composable
private fun ThemePreviewComponent(
    colors: ThemePreviewColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.background)
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PreviewLine(
            color = colors.firstLine,
            widthFraction = 0.55f,
            height = 7.dp,
        )
        PreviewLine(
            color = colors.otherLines,
            widthFraction = 0.85f,
            height = 5.dp,
        )
        PreviewLine(
            color = colors.otherLines,
            widthFraction = 0.7f,
            height = 5.dp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(
                    width = 36.dp,
                    height = 12.dp,
                ).clip(RoundedCornerShape(6.dp))
                .background(colors.accent),
        )
    }
}

@Composable
private fun PreviewLine(
    color: Color,
    widthFraction: Float,
    height: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(3.dp))
            .background(color),
    )
}

// Why: only the switch toggles, as in Bible Planner, so the info button beside the title stays its own control.
@Composable
private fun DynamicColorsRow(
    isOn: Boolean,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    val title = stringResource(R.string.profile_dynamic_colors)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = { onEvent(ProfileUiEvent.OnDynamicColorsInfoClicked) }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.profile_dynamic_colors_info),
                    )
                }
            }
            Text(
                text = stringResource(R.string.profile_dynamic_colors_detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = isOn,
            onCheckedChange = { onEvent(ProfileUiEvent.OnDynamicColorsToggled(it)) },
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

private class ThemePreviewColors(
    val background: Brush,
    val firstLine: Color,
    val otherLines: Color,
    val accent: Color,
)

// Why: Sistema is half light, half dark, since it follows the device.
private fun ThemePreference.toPreviewColors(
    lightScheme: ColorScheme,
    darkScheme: ColorScheme,
): ThemePreviewColors = when (this) {
    ThemePreference.LIGHT -> lightScheme.toPreviewColors()

    ThemePreference.DARK -> darkScheme.toPreviewColors()

    ThemePreference.SYSTEM -> ThemePreviewColors(
        background = Brush.linearGradient(
            0.5f to lightScheme.surfaceContainerHigh,
            0.5f to darkScheme.surfaceContainerHigh,
        ),
        firstLine = lightScheme.outline,
        otherLines = lightScheme.outline.copy(alpha = 0.7f),
        accent = lightScheme.primary,
    )
}

private fun ColorScheme.toPreviewColors(): ThemePreviewColors = ThemePreviewColors(
    background = SolidColor(surfaceContainerHigh),
    firstLine = onSurfaceVariant,
    otherLines = onSurfaceVariant.copy(alpha = 0.7f),
    accent = primary,
)

private val ThemePreference.icon: ImageVector
    get() = when (this) {
        ThemePreference.SYSTEM -> Icons.Outlined.SettingsBrightness
        ThemePreference.LIGHT -> Icons.Outlined.LightMode
        ThemePreference.DARK -> Icons.Outlined.DarkMode
    }
