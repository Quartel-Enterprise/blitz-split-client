package com.quare.blitzsplit.core.provider.image.di

import com.quare.blitzsplit.core.provider.image.AvatarImageEncoder
import com.quare.blitzsplit.core.provider.image.createAvatarImageEncoder
import org.koin.core.module.Module
import org.koin.dsl.module

val imageModule: Module = module {
    single<AvatarImageEncoder> { createAvatarImageEncoder() }
}
