package com.quare.blitzsplit.core.inappupdate.di

import com.quare.blitzsplit.core.inappupdate.data.repository.NoUpdateRepository
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateMechanism
import com.quare.blitzsplit.core.inappupdate.domain.repository.AppUpdateRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

// Why: the web always serves the latest version, and the jvm target only runs the tests.
internal val noUpdateModule: Module = module {
    single { UpdateMechanism.NONE }
    singleOf(::NoUpdateRepository).bind<AppUpdateRepository>()
}
