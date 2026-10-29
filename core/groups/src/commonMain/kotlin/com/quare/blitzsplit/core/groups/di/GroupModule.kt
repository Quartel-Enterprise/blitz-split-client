package com.quare.blitzsplit.core.groups.di

import com.quare.blitzsplit.core.groups.data.datasource.GroupLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupRemoteDataSource
import com.quare.blitzsplit.core.groups.data.datasource.RoomGroupLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.SupabaseGroupRemoteDataSource
import com.quare.blitzsplit.core.groups.data.mapper.GroupMapper
import com.quare.blitzsplit.core.groups.data.mapper.GroupMapperImpl
import com.quare.blitzsplit.core.groups.data.repository.GroupRepositoryImpl
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.CheckMemberName
import com.quare.blitzsplit.core.groups.domain.usecase.CreateGroup
import com.quare.blitzsplit.core.groups.domain.usecase.EncodeQrCode
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CheckMemberNameUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CreateGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.EncodeQrCodeUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val groupModule = module {
    singleOf(::RoomGroupLocalDataSource).bind<GroupLocalDataSource>()
    singleOf(::SupabaseGroupRemoteDataSource).bind<GroupRemoteDataSource>()
    singleOf(::GroupMapperImpl).bind<GroupMapper>()
    singleOf(::GroupRepositoryImpl).bind<GroupRepository>()
    factoryOf(::ObserveGroupsUseCase).bind<ObserveGroups>()
    factoryOf(::CreateGroupUseCase).bind<CreateGroup>()
    factoryOf(::CheckMemberNameUseCase).bind<CheckMemberName>()
    factoryOf(::ObserveGroupUseCase).bind<ObserveGroup>()
    factoryOf(::EncodeQrCodeUseCase).bind<EncodeQrCode>()
}
