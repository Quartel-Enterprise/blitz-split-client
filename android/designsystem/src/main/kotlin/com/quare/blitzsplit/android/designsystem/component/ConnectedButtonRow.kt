package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Why: the connected button group of Material 3 Expressive, as the canvas draws it: equal widths, the outer corners
 * round, the inner ones tight, and the selected button a full pill. It serves as the group's tabs (Despesas, Saldos,
 * Membros) and as Dividir's modes (Igual, Valores, %, Partes).
 */
@Composable
fun ConnectedButtonRow(
    labels: List<String>,
    selectedIndex: Int,
    groupLabel: String,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemRole: Role = Role.Tab,
    height: Dp = 48.dp,
    innerRadius: Dp = 8.dp,
    showsCheck: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .semantics { contentDescription = groupLabel },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Surface(
                onClick = { onClick(index) },
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .semantics {
                        role = itemRole
                        selected = isSelected
                    },
                shape = connectedShapeOf(
                    index = index,
                    count = labels.size,
                    isSelected = isSelected,
                    outerRadius = height / 2,
                    innerRadius = innerRadius,
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isSelected && showsCheck) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(18.dp),
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun connectedShapeOf(
    index: Int,
    count: Int,
    isSelected: Boolean,
    outerRadius: Dp,
    innerRadius: Dp,
): Shape {
    if (isSelected) return RoundedCornerShape(outerRadius)
    val start = if (index == 0) outerRadius else innerRadius
    val end = if (index == count - 1) outerRadius else innerRadius
    return RoundedCornerShape(
        topStart = start,
        bottomStart = start,
        topEnd = end,
        bottomEnd = end,
    )
}
