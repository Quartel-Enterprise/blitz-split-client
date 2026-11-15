package com.quare.blitzsplit.core.support.domain.model

/*
 * Why: the email "Open email" starts. The link carries the subject and the body for whoever only takes a mailto link;
 * they also go apart, as written, for the email apps that read them from the platform's own fields instead.
 */
data class SupportEmail(
    val mailtoLink: String,
    val subject: String,
    val body: String,
)
