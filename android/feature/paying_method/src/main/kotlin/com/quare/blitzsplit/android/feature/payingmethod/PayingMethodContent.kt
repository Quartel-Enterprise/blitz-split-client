package com.quare.blitzsplit.android.feature.payingmethod

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.CardColorPicker
import com.quare.blitzsplit.android.designsystem.component.ChoiceChipRow
import com.quare.blitzsplit.android.designsystem.component.FlatListItem
import com.quare.blitzsplit.android.designsystem.component.PayingMethodTile
import com.quare.blitzsplit.android.designsystem.component.ShimmerFormSkeleton
import com.quare.blitzsplit.android.designsystem.component.nameRes
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.feature.payingmethod.presentation.model.PayingMethodUiEvent
import com.quare.blitzsplit.feature.payingmethod.presentation.model.PayingMethodUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private const val SKELETON_FIELDS = 3
private val singleColumnMaxWidth = 592.dp
private val dialogWidth = 560.dp
private val rowShape = RoundedCornerShape(20.dp)

@Composable
fun PayingMethodContent(
    state: PayingMethodUiState,
    // Why: a tablet draws the method as a 560dp dialog over the profile, always in one column.
    isDialog: Boolean,
    onEvent: (PayingMethodUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isDialog) {
        Surface(
            modifier = modifier
                .widthIn(max = dialogWidth)
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column {
                PayingMethodTopBar(
                    state = state,
                    onEvent = onEvent,
                    isDialog = true,
                )
                PayingMethodFormColumn(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier.imePadding(),
                )
            }
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                PayingMethodTopBar(
                    state = state,
                    onEvent = onEvent,
                    isDialog = false,
                )
            },
        ) { padding ->
            // Why: the keyboard's inset holds the navigation bar's, which the Scaffold's padding already counted.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                PayingMethodFormColumn(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier.widthIn(max = singleColumnMaxWidth),
                )
            }
        }
    }
}

// Why: a saved method fills the form once read, so the form shimmers instead of flashing empty first.
@Composable
private fun PayingMethodFormColumn(
    state: PayingMethodUiState,
    onEvent: (PayingMethodUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoaded) {
        ShimmerFormSkeleton(
            modifier = modifier.padding(16.dp),
            fieldCount = SKELETON_FIELDS,
        )
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 32.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PayingMethodFieldsColumn(
            state = state,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun ColumnScope.PayingMethodFieldsColumn(
    state: PayingMethodUiState,
    onEvent: (PayingMethodUiEvent) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PayingMethodTile(
            type = state.type,
            cardColor = state.cardColor,
            size = 56.dp,
        )
        Text(
            text = state.name.trim().ifEmpty { stringResource(state.type.nameRes) },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
    FormSection(title = stringResource(R.string.paying_method_type)) {
        ChoiceChipRow(
            labels = state.types.map { stringResource(it.nameRes) },
            selectedIndex = state.types.indexOf(state.type),
            groupLabel = stringResource(R.string.paying_method_type),
            onClick = { index -> onEvent(PayingMethodUiEvent.OnTypeSelected(state.types[index])) },
        )
    }
    OutlinedTextField(
        value = state.name,
        onValueChange = { onEvent(PayingMethodUiEvent.OnNameChanged(it.take(state.nameMaxLength))) },
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                stringResource(
                    if (state.isNameRequired) R.string.paying_method_name else R.string.paying_method_name_optional,
                ),
            )
        },
        supportingText = { Text("${state.name.length}/${state.nameMaxLength}") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
    )
    if (state.showsCardColors) {
        FormSection(title = stringResource(R.string.paying_method_color)) {
            CardColorPicker(
                colors = state.cardColors,
                selected = state.cardColor,
                onSelected = { onEvent(PayingMethodUiEvent.OnCardColorSelected(it)) },
            )
        }
    }
    DefaultMethodRow(
        isDefault = state.isDefault,
        onEvent = onEvent,
    )
    if (state.isEditing) {
        OutlinedButton(
            onClick = { onEvent(PayingMethodUiEvent.OnDeleteClicked) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.paying_method_delete),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

// Why: "Default method", what a new bill the person pays opens on; none is fine, so it is never locked.
@Composable
private fun DefaultMethodRow(
    isDefault: Boolean,
    onEvent: (PayingMethodUiEvent) -> Unit,
) {
    FlatListItem(
        headlineContent = {
            Text(
                text = stringResource(R.string.paying_method_default),
                fontWeight = FontWeight.SemiBold,
            )
        },
        supportingContent = { Text(stringResource(R.string.paying_method_default_hint)) },
        trailingContent = {
            Switch(
                checked = isDefault,
                onCheckedChange = null,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .clip(rowShape)
            .toggleable(
                value = isDefault,
                role = Role.Switch,
                onValueChange = { onEvent(PayingMethodUiEvent.OnDefaultChanged(it)) },
            ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayingMethodTopBar(
    state: PayingMethodUiState,
    onEvent: (PayingMethodUiEvent) -> Unit,
    isDialog: Boolean,
) {
    TopAppBar(
        title = {
            Text(
                stringResource(
                    if (state.isEditing) R.string.paying_method_edit_title else R.string.paying_method_new_title,
                ),
            )
        },
        navigationIcon = {
            IconButton(onClick = { onEvent(PayingMethodUiEvent.OnBackClicked) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(DesignR.string.designsystem_back),
                )
            }
        },
        actions = {
            Button(
                onClick = { onEvent(PayingMethodUiEvent.OnSaveClicked) },
                enabled = state.canSave,
                modifier = Modifier.padding(end = 12.dp),
            ) { Text(stringResource(R.string.paying_method_save)) }
        },
        windowInsets = if (isDialog) WindowInsets(0) else TopAppBarDefaults.windowInsets,
        colors = if (isDialog) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        },
    )
}

@DevicePreviews
@Composable
private fun PayingMethodCardPreview() {
    BlitzSplitTheme {
        PayingMethodContent(
            state = PayingMethodUiState(
                isEditing = true,
                isLoaded = true,
                types = PayingMethodType.entries,
                type = PayingMethodType.CREDIT_CARD,
                name = "Travel card",
                cardColor = CardColor.PURPLE,
                isDefault = true,
                isSaving = false,
                isDeleteConfirmationShown = false,
                openedInput = null,
            ),
            isDialog = false,
            onEvent = {},
        )
    }
}
