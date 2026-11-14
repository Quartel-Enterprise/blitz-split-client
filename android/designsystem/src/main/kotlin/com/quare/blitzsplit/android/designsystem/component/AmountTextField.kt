package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/*
 * Why: the large amount of Rachar conta and Registrar pagamento, the currency's symbol before it, the number
 * keyboard, and what it means under it, read out as it changes. With `onSymbolClick`, tapping the symbol opens what
 * changes the currency (Rachar conta's menu beside the total, beta feedback), labeled `symbolClickLabel`.
 */
@Composable
fun AmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    symbol: String,
    help: String?,
    isError: Boolean,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
    onSymbolClick: (() -> Unit)? = null,
    symbolClickLabel: String? = null,
) {
    val amountField = rememberNumberField(value, onValueChange)
    OutlinedTextField(
        value = amountField.value,
        onValueChange = amountField.onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        textStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
        label = { Text(label) },
        prefix = {
            Text(
                text = symbol,
                modifier = Modifier
                    .then(
                        if (onSymbolClick != null && enabled) {
                            Modifier.clickable(
                                onClickLabel = symbolClickLabel,
                                role = Role.Button,
                                onClick = onSymbolClick,
                            )
                        } else {
                            Modifier
                        },
                    ).padding(end = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        supportingText = help?.let { text ->
            {
                Text(
                    text = text,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
        ),
    )
}
