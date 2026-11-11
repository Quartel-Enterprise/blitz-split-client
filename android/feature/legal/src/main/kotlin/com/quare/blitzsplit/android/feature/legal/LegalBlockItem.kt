package com.quare.blitzsplit.android.feature.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.core.legal.domain.model.LegalBlock
import com.quare.blitzsplit.core.legal.domain.model.LegalLink
import com.quare.blitzsplit.core.legal.domain.model.LegalTableRow
import com.quare.blitzsplit.core.legal.domain.model.LegalText

private const val BULLET = "•"

@Composable
internal fun LegalBlockItem(
    block: LegalBlock,
    onLinkClick: (LegalLink) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (block) {
        is LegalBlock.Section -> SectionTitle(
            section = block,
            onLinkClick = onLinkClick,
            modifier = modifier,
        )

        is LegalBlock.Heading -> LegalRichText(
            text = block.text,
            onLinkClick = onLinkClick,
            style = MaterialTheme.typography.titleMedium,
            modifier = modifier
                .padding(top = 8.dp)
                .semantics { heading() },
        )

        is LegalBlock.Paragraph -> LegalRichText(
            text = block.text,
            onLinkClick = onLinkClick,
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier,
        )

        is LegalBlock.Bullets -> BulletList(
            items = block.items,
            onLinkClick = onLinkClick,
            modifier = modifier,
        )

        is LegalBlock.Callout -> CalloutCard(
            callout = block,
            onLinkClick = onLinkClick,
            modifier = modifier,
        )

        is LegalBlock.Table -> TableRowsColumn(
            table = block,
            onLinkClick = onLinkClick,
            modifier = modifier,
        )
    }
}

// Why: the section's number beside its title, as the site draws it, the title wrapping clear of the number.
@Composable
private fun SectionTitle(
    section: LegalBlock.Section,
    onLinkClick: (LegalLink) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = section.number.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        LegalRichText(
            text = section.title,
            onLinkClick = onLinkClick,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
    }
}

@Composable
private fun BulletList(
    items: List<LegalText>,
    onLinkClick: (LegalLink) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = BULLET,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clearAndSetSemantics {},
                )
                LegalRichText(
                    text = item,
                    onLinkClick = onLinkClick,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// Why: the summary that opens the text, or a note inside a section, in a tinted card.
@Composable
private fun CalloutCard(
    callout: LegalBlock.Callout,
    onLinkClick: (LegalLink) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LegalRichText(
                text = callout.title,
                onLinkClick = onLinkClick,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            callout.paragraph?.let { paragraph ->
                LegalRichText(
                    text = paragraph,
                    onLinkClick = onLinkClick,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (callout.items.isNotEmpty()) {
                BulletList(
                    items = callout.items,
                    onLinkClick = onLinkClick,
                )
            }
        }
    }
}

// Why: a table reads as cards at every width, as the site's phone layout: the column is 640 dp at most.
@Composable
private fun TableRowsColumn(
    table: LegalBlock.Table,
    onLinkClick: (LegalLink) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        table.rows.forEach { row ->
            TableRowCard(
                header = table.header,
                row = row,
                onLinkClick = onLinkClick,
            )
        }
    }
}

@Composable
private fun TableRowCard(
    header: List<LegalText>,
    row: LegalTableRow,
    onLinkClick: (LegalLink) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.cells.forEachIndexed { index, cell ->
                when {
                    index == 0 -> LegalRichText(
                        text = cell,
                        onLinkClick = onLinkClick,
                        style = MaterialTheme.typography.titleMedium,
                    )

                    // Why: a cell the row leaves empty would be a column's name over nothing.
                    cell.plain.isNotBlank() -> TableCellColumn(
                        label = header.getOrNull(index),
                        cell = cell,
                        onLinkClick = onLinkClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun TableCellColumn(
    label: LegalText?,
    cell: LegalText,
    onLinkClick: (LegalLink) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        label?.let {
            Text(
                text = it.plain,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LegalRichText(
            text = cell,
            onLinkClick = onLinkClick,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
