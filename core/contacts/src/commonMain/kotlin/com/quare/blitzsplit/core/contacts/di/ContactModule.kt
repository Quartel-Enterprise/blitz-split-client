package com.quare.blitzsplit.core.contacts.di

import com.quare.blitzsplit.core.contacts.data.datasource.GuestMergeRemoteDataSource
import com.quare.blitzsplit.core.contacts.data.datasource.HiddenContactLocalDataSource
import com.quare.blitzsplit.core.contacts.data.datasource.HiddenContactRemoteDataSource
import com.quare.blitzsplit.core.contacts.data.datasource.RoomHiddenContactLocalDataSource
import com.quare.blitzsplit.core.contacts.data.datasource.SupabaseGuestMergeRemoteDataSource
import com.quare.blitzsplit.core.contacts.data.datasource.SupabaseHiddenContactRemoteDataSource
import com.quare.blitzsplit.core.contacts.data.repository.GuestMergeRepositoryImpl
import com.quare.blitzsplit.core.contacts.data.repository.HiddenContactRepositoryImpl
import com.quare.blitzsplit.core.contacts.domain.repository.GuestMergeRepository
import com.quare.blitzsplit.core.contacts.domain.repository.HiddenContactRepository
import com.quare.blitzsplit.core.contacts.domain.usecase.AddContactToGroup
import com.quare.blitzsplit.core.contacts.domain.usecase.CancelGuestMerge
import com.quare.blitzsplit.core.contacts.domain.usecase.FindPendingGuestMerge
import com.quare.blitzsplit.core.contacts.domain.usecase.ObserveContactBook
import com.quare.blitzsplit.core.contacts.domain.usecase.RequestGuestMerge
import com.quare.blitzsplit.core.contacts.domain.usecase.SetContactHidden
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.AddContactToGroupUseCase
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.CancelGuestMergeUseCase
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.FindPendingGuestMergeUseCase
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.ObserveContactBookUseCase
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.RequestGuestMergeUseCase
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.SetContactHiddenUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val contactModule = module {
    singleOf(::RoomHiddenContactLocalDataSource).bind<HiddenContactLocalDataSource>()
    singleOf(::SupabaseHiddenContactRemoteDataSource).bind<HiddenContactRemoteDataSource>()
    singleOf(::HiddenContactRepositoryImpl).bind<HiddenContactRepository>()
    singleOf(::SupabaseGuestMergeRemoteDataSource).bind<GuestMergeRemoteDataSource>()
    singleOf(::GuestMergeRepositoryImpl).bind<GuestMergeRepository>()
    factoryOf(::ObserveContactBookUseCase).bind<ObserveContactBook>()
    factoryOf(::SetContactHiddenUseCase).bind<SetContactHidden>()
    factoryOf(::AddContactToGroupUseCase).bind<AddContactToGroup>()
    factoryOf(::RequestGuestMergeUseCase).bind<RequestGuestMerge>()
    factoryOf(::FindPendingGuestMergeUseCase).bind<FindPendingGuestMerge>()
    factoryOf(::CancelGuestMergeUseCase).bind<CancelGuestMerge>()
}
