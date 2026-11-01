package com.quare.blitzsplit.core.groups.di

import com.quare.blitzsplit.core.groups.data.datasource.GroupLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupPinLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupPinRemoteDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupRemoteDataSource
import com.quare.blitzsplit.core.groups.data.datasource.RoomGroupLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.RoomGroupPinLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.SupabaseGroupPinRemoteDataSource
import com.quare.blitzsplit.core.groups.data.datasource.SupabaseGroupRemoteDataSource
import com.quare.blitzsplit.core.groups.data.mapper.GroupMapper
import com.quare.blitzsplit.core.groups.data.mapper.GroupMapperImpl
import com.quare.blitzsplit.core.groups.data.mapper.InviteMapper
import com.quare.blitzsplit.core.groups.data.mapper.InviteMapperImpl
import com.quare.blitzsplit.core.groups.data.repository.GroupPinRepositoryImpl
import com.quare.blitzsplit.core.groups.data.repository.GroupRepositoryImpl
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.data.repository.InviteRepositoryImpl
import com.quare.blitzsplit.core.groups.domain.repository.GroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.repository.GroupPinRepository
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.repository.InviteRepository
import com.quare.blitzsplit.core.groups.domain.usecase.AddMember
import com.quare.blitzsplit.core.groups.domain.usecase.CheckMemberName
import com.quare.blitzsplit.core.groups.domain.usecase.ConsumeGroupDeletion
import com.quare.blitzsplit.core.groups.domain.usecase.CreateGroup
import com.quare.blitzsplit.core.groups.domain.usecase.EncodeQrCode
import com.quare.blitzsplit.core.groups.domain.usecase.GetInvite
import com.quare.blitzsplit.core.groups.domain.usecase.GetSplitIncomes
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroupDeletions
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.groups.domain.usecase.ObservePinnedGroupIds
import com.quare.blitzsplit.core.groups.domain.usecase.ReleaseMember
import com.quare.blitzsplit.core.groups.domain.usecase.RemoveMember
import com.quare.blitzsplit.core.groups.domain.usecase.RenameMember
import com.quare.blitzsplit.core.groups.domain.usecase.SetGroupPinned
import com.quare.blitzsplit.core.groups.domain.usecase.UpdateDefaultSplit
import com.quare.blitzsplit.core.groups.domain.usecase.UpdateGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.AddMemberUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CheckMemberNameUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeGroupDeletionUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CreateGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.EncodeQrCodeUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.GetInviteUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.GetSplitIncomesUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupDeletionsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObservePinnedGroupIdsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ReleaseMemberUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.RemoveMemberUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.RenameMemberUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.SetGroupPinnedUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.UpdateDefaultSplitUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.UpdateGroupUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val groupModule = module {
    singleOf(::RoomGroupLocalDataSource).bind<GroupLocalDataSource>()
    singleOf(::SupabaseGroupRemoteDataSource).bind<GroupRemoteDataSource>()
    singleOf(::GroupMapperImpl).bind<GroupMapper>()
    singleOf(::GroupRepositoryImpl).bind<GroupRepository>()
    singleOf(::InMemoryGroupDeletionRepository).bind<GroupDeletionRepository>()
    singleOf(::InviteMapperImpl).bind<InviteMapper>()
    singleOf(::InviteRepositoryImpl).bind<InviteRepository>()
    factoryOf(::ObserveGroupsUseCase).bind<ObserveGroups>()
    factoryOf(::CreateGroupUseCase).bind<CreateGroup>()
    factoryOf(::CheckMemberNameUseCase).bind<CheckMemberName>()
    factoryOf(::ObserveGroupUseCase).bind<ObserveGroup>()
    factoryOf(::EncodeQrCodeUseCase).bind<EncodeQrCode>()
    factoryOf(::AddMemberUseCase).bind<AddMember>()
    factoryOf(::RenameMemberUseCase).bind<RenameMember>()
    factoryOf(::RemoveMemberUseCase).bind<RemoveMember>()
    factoryOf(::ReleaseMemberUseCase).bind<ReleaseMember>()
    factoryOf(::UpdateGroupUseCase).bind<UpdateGroup>()
    factoryOf(::UpdateDefaultSplitUseCase).bind<UpdateDefaultSplit>()
    factoryOf(::GetSplitIncomesUseCase).bind<GetSplitIncomes>()
    factoryOf(::GetInviteUseCase).bind<GetInvite>()
    factoryOf(::ObserveGroupDeletionsUseCase).bind<ObserveGroupDeletions>()
    factoryOf(::ConsumeGroupDeletionUseCase).bind<ConsumeGroupDeletion>()
    singleOf(::RoomGroupPinLocalDataSource).bind<GroupPinLocalDataSource>()
    singleOf(::SupabaseGroupPinRemoteDataSource).bind<GroupPinRemoteDataSource>()
    singleOf(::GroupPinRepositoryImpl).bind<GroupPinRepository>()
    factoryOf(::ObservePinnedGroupIdsUseCase).bind<ObservePinnedGroupIds>()
    factoryOf(::SetGroupPinnedUseCase).bind<SetGroupPinned>()
}
