package com.quare.blitzsplit.android.feature.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState

@Composable
internal fun PreferencesSection(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Section(title = R.string.profile_section_preferences) {
        SectionRow(
            icon = Icons.Outlined.Palette,
            headline = stringResource(R.string.profile_appearance),
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
        ) {
            ThemePreference.entries.forEachIndexed { index, theme ->
                SegmentedButton(
                    selected = state.theme == theme,
                    onClick = { onEvent(ProfileUiEvent.OnThemeSelected(theme)) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ThemePreference.entries.size,
                    ),
                ) { Text(stringResource(theme.labelRes)) }
            }
        }
    }
}

@Composable
internal fun AboutSection(
    appVersion: String,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Section(title = R.string.profile_section_about) {
        // Why: works offline and without an account, since it reads the copy on this device.
        SectionRow(
            icon = Icons.Outlined.Download,
            headline = stringResource(R.string.profile_export),
            modifier = Modifier.clickable { onEvent(ProfileUiEvent.OnExportClicked) },
            supporting = stringResource(R.string.profile_export_detail),
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        HorizontalDivider()
        SectionRow(
            icon = Icons.Outlined.Visibility,
            headline = stringResource(R.string.profile_visibility),
            modifier = Modifier.clickable { onEvent(ProfileUiEvent.OnVisibilityInfoClicked) },
        )
        HorizontalDivider()
        SectionRow(
            icon = Icons.AutoMirrored.Outlined.OpenInNew,
            headline = stringResource(R.string.profile_privacy_policy),
            modifier = Modifier.clickable { onEvent(ProfileUiEvent.OnPrivacyPolicyClicked) },
        )
        HorizontalDivider()
        SectionRow(
            icon = Icons.AutoMirrored.Outlined.OpenInNew,
            headline = stringResource(R.string.profile_terms),
            modifier = Modifier.clickable { onEvent(ProfileUiEvent.OnTermsClicked) },
        )
        HorizontalDivider()
        SectionRow(
            icon = Icons.Outlined.Info,
            headline = stringResource(R.string.profile_version),
            trailing = {
                Text(
                    text = appVersion,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
    }
}

@Composable
internal fun SignOutButton(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    OutlinedButton(
        onClick = { onEvent(ProfileUiEvent.OnSignOutClicked) },
        enabled = state.canSignOut,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (state.showsDeleteAccount) 0.dp else 16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
            )
            Text(
                stringResource(
                    if (state.hasAccount) R.string.profile_sign_out else R.string.profile_erase_and_sign_out,
                ),
            )
        }
    }
}

/*
 * Why: the stores and the LGPD ask for it apart from "Sair", at the very end. Only a Google or Apple account
 * has one on the server; without one "Apagar perfil e sair" already erases everything.
 */
@Composable
internal fun DeleteAccountButton(onEvent: (ProfileUiEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 12.dp,
                bottom = 16.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TextButton(
            onClick = { onEvent(ProfileUiEvent.OnDeleteAccountClicked) },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteForever,
                    contentDescription = null,
                )
                Text(stringResource(R.string.profile_delete_account))
            }
        }
        Text(
            text = stringResource(R.string.profile_delete_account_detail),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@get:StringRes
private val ThemePreference.labelRes: Int
    get() = when (this) {
        ThemePreference.SYSTEM -> R.string.profile_theme_system
        ThemePreference.LIGHT -> R.string.profile_theme_light
        ThemePreference.DARK -> R.string.profile_theme_dark
    }
