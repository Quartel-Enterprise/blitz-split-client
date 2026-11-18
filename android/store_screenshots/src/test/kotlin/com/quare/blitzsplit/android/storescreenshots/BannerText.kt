package com.quare.blitzsplit.android.storescreenshots

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em

private const val DESCRIPTION_ALPHA = 0.85f
private val bannerLineHeight = 1.2.em

/**
 * A line of the banner as the library draws it, centered across the canvas, at [fontSize]: the title white and bold
 * ([isTitle]), the description under it a dimmer white.
 */
@Composable
internal fun BannerText(
    text: String,
    fontSize: TextUnit,
    isTitle: Boolean,
) {
    Text(
        text = text,
        color = if (isTitle) Color.White else Color.White.copy(alpha = DESCRIPTION_ALPHA),
        fontSize = fontSize,
        lineHeight = bannerLineHeight,
        fontWeight = if (isTitle) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
