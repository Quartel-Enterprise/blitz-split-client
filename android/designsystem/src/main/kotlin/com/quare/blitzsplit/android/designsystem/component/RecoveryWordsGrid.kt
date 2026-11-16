package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme

/*
 * Why: the 12 recovery words, numbered so they are written down in order. They read row by row, the order the restore
 * screen's fields take them in.
 */
@Composable
fun RecoveryWordsGrid(
    words: List<String>,
    modifier: Modifier = Modifier,
    columnCount: Int = 3,
    isCompact: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp),
    ) {
        words.chunked(columnCount).forEachIndexed { row, rowWords ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(columnCount) { column ->
                    Box(modifier = Modifier.weight(1f)) {
                        rowWords.getOrNull(column)?.let { word ->
                            RecoveryWordItem(
                                number = row * columnCount + column + 1,
                                word = word,
                                isCompact = isCompact,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecoveryWordItem(
    number: Int,
    word: String,
    isCompact: Boolean,
) {
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$number $word" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "$number",
            modifier = Modifier.widthIn(min = 20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
        Text(
            text = word,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (isCompact) 15.sp else 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )
    }
}

@DevicePreviews
@Composable
private fun RecoveryWordsGridPreview() {
    BlitzSplitTheme {
        RecoveryWordsGrid(
            words = "legal winner thank year wave sausage worth useful legal winner thank yellow".split(" "),
        )
    }
}
