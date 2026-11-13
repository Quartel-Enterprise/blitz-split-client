package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

private val minAmountFontSize = 11.sp
private val amountFontStep = 1.sp

/*
 * Why: a money amount never breaks nor silently loses digits. A plain one-line Text wrapped after the sign and showed
 * only "+" in a narrow pane or at a large font, so this one never wraps: it shrinks to the width it gets, down to
 * 11 sp (the smallest label the screens draw), and only then ends in an ellipsis.
 */
@Composable
fun AmountText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
) {
    val maxFontSize = style.fontSize
    Text(
        text = text,
        modifier = modifier,
        color = color,
        autoSize = if (maxFontSize.isSpecified && maxFontSize > minAmountFontSize) {
            TextAutoSize.StepBased(
                minFontSize = minAmountFontSize,
                maxFontSize = maxFontSize,
                stepSize = amountFontStep,
            )
        } else {
            null
        },
        fontWeight = fontWeight,
        textAlign = textAlign,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
        maxLines = 1,
        style = style,
    )
}
