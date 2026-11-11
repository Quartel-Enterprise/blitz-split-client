package com.quare.blitzsplit.core.legal.data.parser

import com.quare.blitzsplit.core.legal.domain.model.LegalLink
import com.quare.blitzsplit.core.legal.domain.model.LegalSpan
import com.quare.blitzsplit.core.legal.domain.model.LegalText
import com.quare.blitzsplit.core.model.LegalPage

/*
 * Why: a line's **bold**, [links](address) and <mark>what the text still has to decide</mark>, which reads as plain
 * text here. A link whose address the app can't open (anything but https, mailto and the legal pages) keeps its words
 * without the link.
 */
internal object LegalInlineParser {
    private val TOKEN = Regex("""\*\*(.+?)\*\*|\[([^\]]+)]\(([^)\s]+)\)|<mark>(.+?)</mark>""")
    private val PAGE_ADDRESS = Regex("""/([a-z]+)(?:#([a-z0-9-]+))?""")
    private const val SITE = "https://blitzsplit.com"
    private const val BOLD_GROUP = 1
    private const val LINK_TEXT_GROUP = 2
    private const val LINK_ADDRESS_GROUP = 3
    private const val MARK_GROUP = 4

    fun parse(text: String): LegalText = LegalText(
        parseSpans(
            text = text,
            isBold = false,
            link = null,
        ),
    )

    private fun parseSpans(
        text: String,
        isBold: Boolean,
        link: LegalLink?,
    ): List<LegalSpan> = buildList {
        var rest = 0
        TOKEN.findAll(text).forEach { match ->
            addPlain(text.substring(rest, match.range.first), isBold, link)
            val groups = match.groupValues
            when {
                groups[BOLD_GROUP].isNotEmpty() -> addAll(
                    parseSpans(
                        groups[BOLD_GROUP],
                        isBold = true,
                        link = link,
                    ),
                )

                groups[LINK_TEXT_GROUP].isNotEmpty() -> addAll(
                    parseSpans(
                        groups[LINK_TEXT_GROUP],
                        isBold = isBold,
                        link =
                            link ?: toLink(groups[LINK_ADDRESS_GROUP]),
                    ),
                )

                else -> addAll(
                    parseSpans(
                        groups[MARK_GROUP],
                        isBold = isBold,
                        link = link,
                    ),
                )
            }
            rest = match.range.last + 1
        }
        addPlain(text.substring(rest), isBold, link)
    }

    private fun MutableList<LegalSpan>.addPlain(
        text: String,
        isBold: Boolean,
        link: LegalLink?,
    ) {
        if (text.isNotEmpty()) {
            add(
                LegalSpan(
                    text = text,
                    isBold = isBold,
                    link = link,
                ),
            )
        }
    }

    // Why: a legal page written by its full address (https://blitzsplit.com/privacy) is still the text the app draws.
    private fun toLink(address: String): LegalLink? = toPageLink(address.removePrefix(SITE))
        ?: address.takeIf { it.startsWith("https://") || it.startsWith("mailto:") }?.let(LegalLink::External)

    private fun toPageLink(address: String): LegalLink.Page? {
        val (slug, anchor) = PAGE_ADDRESS.matchEntire(address)?.destructured ?: return null
        val page = LegalPage.fromSlug(slug) ?: return null
        return LegalLink.Page(
            page = page,
            anchor = anchor.takeIf { it.isNotEmpty() },
        )
    }
}
