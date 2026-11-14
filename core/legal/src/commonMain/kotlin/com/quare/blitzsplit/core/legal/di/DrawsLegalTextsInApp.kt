package com.quare.blitzsplit.core.legal.di

// Why: Android, iOS and the Mac draw the legal texts themselves; the web opens the site's pages in another tab.
internal expect val drawsLegalTextsInApp: Boolean
