package com.quare.blitzsplit.core.inappupdate.di

import com.quare.blitzsplit.core.inappupdate.data.AppStoreStorefront
import org.koin.core.module.Module

internal actual val platformInAppUpdateModule: Module = createAppStoreUpdateModule(AppStoreStorefront.MACOS)
