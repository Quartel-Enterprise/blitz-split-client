package com.quare.blitzsplit.android.feature.legal

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.quare.blitzsplit.core.legal.domain.model.LegalLink
import com.quare.blitzsplit.core.legal.domain.model.LegalText

// Why: a line of the text with its bold runs and its links, each link a tap of its own that TalkBack also reaches.
@Composable
internal fun LegalRichText(
    text: LegalText,
    onLinkClick: (LegalLink) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val currentOnLinkClick by rememberUpdatedState(onLinkClick)
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )
    val annotated = remember(text, linkStyles) {
        buildAnnotatedString {
            text.spans.forEachIndexed { index, span ->
                val weight = SpanStyle(fontWeight = if (span.isBold) FontWeight.SemiBold else null)
                val link = span.link
                if (link == null) {
                    withStyle(weight) { append(span.text) }
                } else {
                    val annotation = LinkAnnotation.Clickable(
                        tag = index.toString(),
                        styles = linkStyles,
                        linkInteractionListener = { currentOnLinkClick(link) },
                    )
                    withLink(annotation) { withStyle(weight) { append(span.text) } }
                }
            }
        }
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = color,
    )
}
