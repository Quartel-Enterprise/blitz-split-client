package com.quare.blitzsplit.core.profile.di

import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfileRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.RoomProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.SupabaseProfileRemoteDataSource
import com.quare.blitzsplit.core.profile.data.mapper.ProfileMapper
import com.quare.blitzsplit.core.profile.data.mapper.ProfileMapperImpl
import com.quare.blitzsplit.core.profile.data.repository.ProfileRepositoryImpl
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.SaveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.ValidateDisplayName
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.SaveProfileUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ValidateDisplayNameUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val profileModule = module {
    singleOf(::RoomProfileLocalDataSource).bind<ProfileLocalDataSource>()
    singleOf(::SupabaseProfileRemoteDataSource).bind<ProfileRemoteDataSource>()
    singleOf(::ProfileMapperImpl).bind<ProfileMapper>()
    singleOf(::ProfileRepositoryImpl).bind<ProfileRepository>()
    factoryOf(::ObserveProfileUseCase).bind<ObserveProfile>()
    factoryOf(::SaveProfileUseCase).bind<SaveProfile>()
    factoryOf(::ValidateDisplayNameUseCase).bind<ValidateDisplayName>()
}
