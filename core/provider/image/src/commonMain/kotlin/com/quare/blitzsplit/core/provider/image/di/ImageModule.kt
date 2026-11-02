package com.quare.blitzsplit.core.provider.image.di

import com.quare.blitzsplit.core.provider.image.AvatarImageEncoder
import com.quare.blitzsplit.core.provider.image.BillImageEncoder
import com.quare.blitzsplit.core.provider.image.createAvatarImageEncoder
import com.quare.blitzsplit.core.provider.image.createBillImageEncoder
import org.koin.core.module.Module
import org.koin.dsl.module

val imageModule: Module = module {
    single<AvatarImageEncoder> { createAvatarImageEncoder() }
    single<BillImageEncoder> { createBillImageEncoder() }
}
