package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

/*
 * Why: "Mapa de categorias" between Sugeridas and Todas as categorias (9.0): which word of the description chose the
 * bill's category, and the way to the words that choose it.
 */
@Composable
internal fun CategoryMapCard(
    state: SplitBillUiState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val match = state.categoryMatches.firstOrNull()
    val mapCategory = state.mapCategory
    val detail = if (match != null && mapCategory != null) {
        stringResource(R.string.split_bill_category_map_by_term, categoryName(mapCategory), match.term)
    } else {
        stringResource(R.string.split_bill_category_map_detail)
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable(role = Role.Button) { onEvent(SplitBillUiEvent.OnCategoryMapClicked) },
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Sell,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.split_bill_category_map),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}
