package com.quare.blitzsplit.core.legal.domain.model

/*
 * Why: a legal page as each UI draws it with its own components: the site's Markdown (blitzsplit.com/legal), the one
 * place the text is written, read into blocks. The words come from the site in the reader's language, never from
 * the app's strings.
 */
data class LegalDocument(
    val title: String,
    val updated: LegalDate,
    val blocks: List<LegalBlock>,
)
