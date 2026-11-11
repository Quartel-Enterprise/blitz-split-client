package com.quare.blitzsplit.core.legal.data.parser

import com.quare.blitzsplit.core.legal.domain.model.LegalDocument

/*
 * Why: the site's Markdown (blitz-split-site's docs/legal.md lists what it takes) into the blocks the UIs draw. Null
 * for what isn't a legal text at all (no front matter: a page of the site, an error); anything inside one that this
 * version doesn't know reads as plain text, so a text the site writes later still shows in an app already installed.
 */
fun interface LegalDocumentParser {
    fun parse(markdown: String): LegalDocument?
}
