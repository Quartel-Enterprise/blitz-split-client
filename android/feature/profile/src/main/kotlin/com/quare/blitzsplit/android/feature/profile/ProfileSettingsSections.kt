package com.quare.blitzsplit.android.feature.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.labelRes
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import software.quare.loadable.isLoading
import software.quare.loadable.valueOrNull

@Composable
internal fun PreferencesSection(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Section(title = R.string.profile_section_preferences) {
        // Why: opens Aparência (the theme's previews and "Cores dinâmicas"), as Bible Planner's theme row does.
        SectionRow(
            icon = Icons.Outlined.Palette,
            headline = stringResource(R.string.profile_appearance),
            modifier = Modifier.clickable(enabled = state.isLoaded) { onEvent(ProfileUiEvent.OnAppearanceClicked) },
            supporting = appearanceSummary(state),
            isSupportingLoading = !state.isLoaded,
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        HorizontalDivider()
        // Why: opens Language (System and each language), as Bible Planner's language row does.
        SectionRow(
            icon = Icons.Outlined.Translate,
            headline = stringResource(R.string.profile_language),
            modifier = Modifier.clickable(enabled = state.isLoaded) { onEvent(ProfileUiEvent.OnLanguageClicked) },
            supporting = languageName(state.language),
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
            icon = Icons.Outlined.Notifications,
            headline = stringResource(R.string.profile_notifications),
            modifier = Modifier.clickable { onEvent(ProfileUiEvent.OnNotificationsClicked) },
            supporting = state.activeNotificationCount.valueOrNull()?.let { count ->
                pluralStringResource(R.plurals.profile_notifications_active, count, count)
            },
            isSupportingLoading = state.activeNotificationCount.isLoading(),
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        HorizontalDivider()
        // Why: every bitcoin amount shown and typed in whole sats on this device, groups in bitcoin or not yet.
        SectionRow(
            icon = Icons.Outlined.CurrencyBitcoin,
            headline = stringResource(R.string.profile_bitcoin_in_sats),
            modifier = Modifier.toggleable(
                value = state.showsBitcoinInSats,
                enabled = state.isLoaded,
                role = Role.Switch,
                onValueChange = { onEvent(ProfileUiEvent.OnBitcoinInSatsToggled(it)) },
            ),
            supporting = stringResource(R.string.profile_bitcoin_in_sats_detail),
            trailing = {
                Switch(
                    checked = state.showsBitcoinInSats,
                    onCheckedChange = null,
                    enabled = state.isLoaded,
                )
            },
        )
        HorizontalDivider()
        // Why: "Mapa de categorias" (flow 9), the words that pick a bill's category from its title.
        SectionRow(
            icon = Icons.Outlined.Sell,
            headline = stringResource(R.string.profile_category_map),
            modifier = Modifier.clickable { onEvent(ProfileUiEvent.OnCategoryMapClicked) },
            supporting = stringResource(R.string.profile_category_map_detail),
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}

@Composable
internal fun AboutSection(
    state: ProfileUiState,
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
        // Why: the version itself sits in the legal footer, at the end of the profile (beta feedback 18).
        if (state.showsCheckForUpdates) {
            HorizontalDivider()
            CheckForUpdatesRow(
                appVersion = appVersion,
                isChecking = state.isCheckingForUpdates,
                onEvent = onEvent,
            )
        }
    }
}

/*
 * Why: the legal pages and the version, discreet and centered at the very end of the profile, as Bible Planner's
 * (beta feedback 18).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LegalFooter(
    appVersion: String,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FlowRow(horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { onEvent(ProfileUiEvent.OnPrivacyPolicyClicked) }) {
                Text(stringResource(R.string.profile_privacy_policy))
            }
            Text(
                text = "•",
                modifier = Modifier.align(Alignment.CenterVertically),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { onEvent(ProfileUiEvent.OnTermsClicked) }) {
                Text(stringResource(R.string.profile_terms))
            }
        }
        Text(
            text = stringResource(R.string.profile_check_for_updates_version, appVersion),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// Why: "Buscar atualizações" in the place of "Versão", with the version under it, as Bible Planner's.
@Composable
private fun CheckForUpdatesRow(
    appVersion: String,
    isChecking: Boolean,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    SectionRow(
        icon = Icons.Outlined.SystemUpdate,
        headline = stringResource(R.string.profile_check_for_updates),
        modifier = Modifier.clickable(enabled = !isChecking) { onEvent(ProfileUiEvent.OnCheckForUpdatesClicked) },
        supporting = if (isChecking) {
            stringResource(R.string.profile_check_for_updates_checking)
        } else {
            stringResource(R.string.profile_check_for_updates_version, appVersion)
        },
        trailing = {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
        },
    )
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
            val labelRes = if (state.erasesProfileOnSignOut) {
                R.string.profile_erase_and_sign_out
            } else {
                R.string.profile_sign_out
            }
            Text(stringResource(labelRes))
        }
    }
}

/*
 * Why: the stores and the LGPD ask for it apart from "Sign out", at the very end, for every account the server keeps,
 * an anonymous one too; a local only profile has none, and "Erase profile and sign out" already erases everything.
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

// Why: "Sistema", or "Sistema · cores dinâmicas" while the wallpaper's colors are on.
@Composable
private fun appearanceSummary(state: ProfileUiState): String {
    val theme = stringResource(state.theme.labelRes)
    return if (state.usesDynamicColors) stringResource(R.string.profile_appearance_dynamic_summary, theme) else theme
}

@get:StringRes
internal val ThemePreference.labelRes: Int
    get() = when (this) {
        ThemePreference.SYSTEM -> R.string.profile_theme_system
        ThemePreference.LIGHT -> R.string.profile_theme_light
        ThemePreference.DARK -> R.string.profile_theme_dark
    }
