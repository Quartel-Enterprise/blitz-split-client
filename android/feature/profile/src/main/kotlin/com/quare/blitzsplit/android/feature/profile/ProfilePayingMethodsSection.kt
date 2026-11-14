package com.quare.blitzsplit.android.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.FlatListItem
import com.quare.blitzsplit.android.designsystem.component.PayingMethodTile
import com.quare.blitzsplit.android.designsystem.component.label
import com.quare.blitzsplit.android.designsystem.component.nameRes
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import software.quare.loadable.valueOrNull

// Why: how the person pays, apart from the keys they receive with: one connected row per method, then the one adding.
@Composable
internal fun PayingMethodsSection(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    val methods = state.payingMethods.valueOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_section_paying_methods),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.profile_paying_methods_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (methods == null) {
            PaymentKeysSkeleton()
            return@Column
        }
        val count = methods.size + 1
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            methods.forEachIndexed { index, method ->
                PayingMethodRow(
                    method = method,
                    shape = segmentShapeOf(
                        index = index,
                        count = count,
                    ),
                    onClick = { onEvent(ProfileUiEvent.PayingMethods.OnClicked(method.id)) },
                )
            }
            AddKeySurface(
                label = stringResource(R.string.profile_paying_methods_add),
                shape = segmentShapeOf(
                    index = count - 1,
                    count = count,
                ),
                onClick = { onEvent(ProfileUiEvent.PayingMethods.OnAddClicked) },
            )
        }
    }
}

@Composable
private fun PayingMethodRow(
    method: PayingMethod,
    shape: Shape,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        FlatListItem(
            headlineContent = {
                Text(
                    text = method.label(),
                    fontWeight = FontWeight.SemiBold,
                )
            },
            // Why: a named method says what it is under its name; one without a name already reads as its type.
            supportingContent = if (method.name.isNotBlank()) {
                { Text(stringResource(method.type.nameRes)) }
            } else {
                null
            },
            leadingContent = {
                PayingMethodTile(
                    type = method.type,
                    cardColor = method.cardColor,
                )
            },
            trailingContent = if (method.isDefault) {
                { DefaultKeyBadge() }
            } else {
                null
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        )
    }
}
