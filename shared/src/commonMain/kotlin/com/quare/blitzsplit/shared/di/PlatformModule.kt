package com.quare.blitzsplit.shared.di

import org.koin.core.module.Module

// Why: which sign in each platform does natively, and anything else only one platform has.
internal expect val platformModule: Module
