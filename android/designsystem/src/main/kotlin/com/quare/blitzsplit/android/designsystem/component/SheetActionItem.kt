package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

// Why: one action of a bottom sheet's list (Editar perfil's photo, "Ler cupom fiscal"'s source): its icon and label.
@Composable
fun SheetActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        FlatListItem(
            headlineContent = { Text(label) },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        )
    }
}
