package com.quare.blitzsplit.core.inappupdate.di

import org.koin.core.module.Module

// Why: a watch app updates with the App Store's automatic updates, and the watch never shows the update prompt.
internal actual val platformInAppUpdateModule: Module = noUpdateModule
