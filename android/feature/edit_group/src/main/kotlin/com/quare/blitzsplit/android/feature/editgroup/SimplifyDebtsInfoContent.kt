package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.TwoHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.core.model.GroupCurrency

private val singleColumnMaxWidth = 560.dp
private val twoColumnsMaxWidth = 1016.dp
private val bookGap = 72.dp
private const val EXAMPLE_MAJOR_UNITS = 20L
private const val TOTAL_CENTS = 12_140L
private const val CENTS_PER_UNIT = 100L

/*
 * Why: "Como funciona" of "Simplificar dívidas" (13.2), the same words for everyone with the example's amounts in the
 * group's currency. One column on a phone and upright (560dp); lying down the intro and the example on the left and
 * what to know on the right, a book splitting them on its hinge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplifyDebtsInfoContent(
    currency: GroupCurrency,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter(currency)
    val example = money.toText(EXAMPLE_MAJOR_UNITS * currency.minorPerMajor)
    val total = money.toText(TOTAL_CENTS * currency.minorPerMajor / CENTS_PER_UNIT)
    val formFactor = rememberFoldLayout().formFactor
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.simplify_info_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        val explanation: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                IntroSection()
                ExampleCard(example)
            }
        }
        val facts: @Composable () -> Unit = { FactsColumn(total) }
        when (formFactor) {
            FormFactor.Book -> TwoHalvesLayout(
                start = { ScrollingColumn { explanation() } },
                end = { ScrollingColumn { facts() } },
                modifier = Modifier.padding(padding),
                gap = bookGap,
            )

            FormFactor.PhoneLandscape, FormFactor.TabletLandscape -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Row(
                    modifier = Modifier.widthIn(max = twoColumnsMaxWidth),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) { ScrollingColumn { explanation() } }
                    Box(modifier = Modifier.weight(1f)) { ScrollingColumn { facts() } }
                }
            }

            else -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(modifier = Modifier.widthIn(max = singleColumnMaxWidth)) {
                    ScrollingColumn {
                        explanation()
                        facts()
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollingColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun IntroSection() {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.simplify_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.simplify_info_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Why: "Um exemplo": Carla owes Elisa, who owes you, before and after, the second in the primary container.
@Composable
private fun ExampleCard(amount: String) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.simplify_info_example),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.simplify_info_example_text, amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExampleFigureBox(
                    isSimplified = false,
                    amount = amount,
                    modifier = Modifier.weight(1f),
                )
                ExampleFigureBox(
                    isSimplified = true,
                    amount = amount,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(R.string.simplify_info_example_result, amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExampleFigureBox(
    isSimplified: Boolean,
    amount: String,
    modifier: Modifier = Modifier,
) {
    val payments = if (isSimplified) 1 else 2
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        // Why: the result stands out by its accent edge, not a fill: the accent's container is the blue face's own color.
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = if (isSimplified) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 6.dp,
                    end = 6.dp,
                    top = 10.dp,
                    bottom = 12.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(
                    if (isSimplified) R.string.simplify_info_after else R.string.simplify_info_before,
                ),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SimplifyExampleImage(
                isSimplified = isSimplified,
                amount = amount,
                description = stringResource(
                    if (isSimplified) R.string.simplify_info_after_a11y else R.string.simplify_info_before_a11y,
                    amount,
                ),
            )
            Text(
                text = pluralStringResource(R.plurals.simplify_payments, payments, payments),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// Why: the four things to know, each with its icon, as the board lists them.
@Composable
private fun FactsColumn(total: String) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        FactRow(
            icon = Icons.Outlined.Menu,
            title = stringResource(R.string.simplify_info_total_title),
            text = stringResource(R.string.simplify_info_total_text, total),
        )
        FactRow(
            icon = Icons.Outlined.WarningAmber,
            title = stringResource(R.string.simplify_info_stranger_title),
            text = stringResource(R.string.simplify_info_stranger_text),
        )
        FactRow(
            icon = Icons.Outlined.Group,
            title = stringResource(R.string.simplify_info_anyone_title),
            text = stringResource(R.string.simplify_info_anyone_text),
        )
        FactRow(
            icon = Icons.Outlined.Replay,
            title = stringResource(R.string.simplify_info_undo_title),
            text = stringResource(R.string.simplify_info_undo_text),
        )
    }
}

@Composable
private fun FactRow(
    icon: ImageVector,
    title: String,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
