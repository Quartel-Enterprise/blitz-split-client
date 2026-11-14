package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/*
 * Why: what the page of a link that doesn't open says, a profile's or a bill's: an icon, why, and what may help, laid
 * in the centered column of the screen that calls it, which adds its own buttons under them.
 */
@Composable
fun LinkProblemMessage(
    icon: ImageVector,
    title: String,
    // Why: null where the title says it all.
    body: String?,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = title,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
    body?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
