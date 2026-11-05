package com.quare.blitzsplit.core.profile.di

import com.quare.blitzsplit.core.profile.data.datasource.AppInviteLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.AppInviteRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.MemberPaymentKeyLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.NotificationPreferencesLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.NotificationPreferencesRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.PaymentKeyLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.PaymentKeyRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfilePhotoRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfileRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.PushDeviceRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.RoomAppInviteLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.RoomMemberPaymentKeyLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.RoomNotificationPreferencesLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.RoomPaymentKeyLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.RoomProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.SupabaseAppInviteRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.SupabaseNotificationPreferencesRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.SupabasePaymentKeyRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.SupabaseProfilePhotoRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.SupabaseProfileRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.SupabasePushDeviceRemoteDataSource
import com.quare.blitzsplit.core.profile.data.mapper.NotificationPreferencesMapper
import com.quare.blitzsplit.core.profile.data.mapper.NotificationPreferencesMapperImpl
import com.quare.blitzsplit.core.profile.data.mapper.PaymentKeyMapper
import com.quare.blitzsplit.core.profile.data.mapper.PaymentKeyMapperImpl
import com.quare.blitzsplit.core.profile.data.mapper.ProfileMapper
import com.quare.blitzsplit.core.profile.data.mapper.ProfileMapperImpl
import com.quare.blitzsplit.core.profile.data.mapper.PushDeviceMapper
import com.quare.blitzsplit.core.profile.data.mapper.PushDeviceMapperImpl
import com.quare.blitzsplit.core.profile.data.repository.AppInviteRepositoryImpl
import com.quare.blitzsplit.core.profile.data.repository.NotificationPreferencesRepositoryImpl
import com.quare.blitzsplit.core.profile.data.repository.PaymentKeyRepositoryImpl
import com.quare.blitzsplit.core.profile.data.repository.ProfileRepositoryImpl
import com.quare.blitzsplit.core.profile.data.repository.PushDeviceRepositoryImpl
import com.quare.blitzsplit.core.profile.domain.model.AppStores
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.profile.domain.model.currentClientPlatform
import com.quare.blitzsplit.core.profile.domain.repository.AppInviteRepository
import com.quare.blitzsplit.core.profile.domain.repository.NotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.profile.domain.repository.PushDeviceRepository
import com.quare.blitzsplit.core.profile.domain.usecase.DeletePaymentKey
import com.quare.blitzsplit.core.profile.domain.usecase.DetectPaymentKeyType
import com.quare.blitzsplit.core.profile.domain.usecase.DismissAppInvite
import com.quare.blitzsplit.core.profile.domain.usecase.FormatPaymentKey
import com.quare.blitzsplit.core.profile.domain.usecase.FormatPaymentKeyInput
import com.quare.blitzsplit.core.profile.domain.usecase.LoadCropPhoto
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveAppInvite
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveMemberPaymentKeys
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveNotificationPreferences
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePaymentKeys
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePendingNotificationPreferenceChanges
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePendingPaymentKeyChanges
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePendingProfileChanges
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import com.quare.blitzsplit.core.profile.domain.usecase.SavePaymentKey
import com.quare.blitzsplit.core.profile.domain.usecase.SaveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.SetPushDevice
import com.quare.blitzsplit.core.profile.domain.usecase.UpdateNotificationPreference
import com.quare.blitzsplit.core.profile.domain.usecase.ValidateDisplayName
import com.quare.blitzsplit.core.profile.domain.usecase.ValidatePaymentKey
import com.quare.blitzsplit.core.profile.domain.usecase.impl.DeletePaymentKeyUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.DetectPaymentKeyTypeUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.DismissAppInviteUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.FormatPaymentKeyInputUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.FormatPaymentKeyUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.LoadCropPhotoUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveAppInviteUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveMemberPaymentKeysUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveNotificationPreferencesUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePaymentKeysUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePendingNotificationPreferenceChangesUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePendingPaymentKeyChangesUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePendingProfileChangesUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.PrepareProfilePhotoUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.SavePaymentKeyUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.SaveProfileUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.SetPushDeviceUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.UpdateNotificationPreferenceUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ValidateDisplayNameUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ValidatePaymentKeyUseCase
import com.quare.blitzsplit.core.profile.generated.StoreConfig
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val profileModule = module {
    singleOf(::RoomProfileLocalDataSource).bind<ProfileLocalDataSource>()
    singleOf(::SupabaseProfileRemoteDataSource).bind<ProfileRemoteDataSource>()
    singleOf(::SupabaseProfilePhotoRemoteDataSource).bind<ProfilePhotoRemoteDataSource>()
    singleOf(::ProfileMapperImpl).bind<ProfileMapper>()
    singleOf(::ProfileRepositoryImpl).bind<ProfileRepository>()
    factoryOf(::ObserveProfileUseCase).bind<ObserveProfile>()
    factoryOf(::ObservePendingProfileChangesUseCase).bind<ObservePendingProfileChanges>()
    factoryOf(::SaveProfileUseCase).bind<SaveProfile>()
    factoryOf(::LoadCropPhotoUseCase).bind<LoadCropPhoto>()
    factoryOf(::PrepareProfilePhotoUseCase).bind<PrepareProfilePhoto>()
    factoryOf(::ValidateDisplayNameUseCase).bind<ValidateDisplayName>()
    singleOf(::RoomPaymentKeyLocalDataSource).bind<PaymentKeyLocalDataSource>()
    singleOf(::RoomMemberPaymentKeyLocalDataSource).bind<MemberPaymentKeyLocalDataSource>()
    singleOf(::SupabasePaymentKeyRemoteDataSource).bind<PaymentKeyRemoteDataSource>()
    singleOf(::PaymentKeyMapperImpl).bind<PaymentKeyMapper>()
    singleOf(::PaymentKeyRepositoryImpl).bind<PaymentKeyRepository>()
    factoryOf(::ObservePaymentKeysUseCase).bind<ObservePaymentKeys>()
    factoryOf(::ObserveMemberPaymentKeysUseCase).bind<ObserveMemberPaymentKeys>()
    factoryOf(::ObservePendingPaymentKeyChangesUseCase).bind<ObservePendingPaymentKeyChanges>()
    factoryOf(::SavePaymentKeyUseCase).bind<SavePaymentKey>()
    factoryOf(::DeletePaymentKeyUseCase).bind<DeletePaymentKey>()
    factoryOf(::ValidatePaymentKeyUseCase).bind<ValidatePaymentKey>()
    factoryOf(::DetectPaymentKeyTypeUseCase).bind<DetectPaymentKeyType>()
    factoryOf(::FormatPaymentKeyInputUseCase).bind<FormatPaymentKeyInput>()
    factoryOf(::FormatPaymentKeyUseCase).bind<FormatPaymentKey>()
    singleOf(::RoomNotificationPreferencesLocalDataSource).bind<NotificationPreferencesLocalDataSource>()
    singleOf(::SupabaseNotificationPreferencesRemoteDataSource).bind<NotificationPreferencesRemoteDataSource>()
    singleOf(::NotificationPreferencesMapperImpl).bind<NotificationPreferencesMapper>()
    singleOf(::NotificationPreferencesRepositoryImpl).bind<NotificationPreferencesRepository>()
    factoryOf(::ObserveNotificationPreferencesUseCase).bind<ObserveNotificationPreferences>()
    factoryOf(::ObservePendingNotificationPreferenceChangesUseCase).bind<ObservePendingNotificationPreferenceChanges>()
    factoryOf(::UpdateNotificationPreferenceUseCase).bind<UpdateNotificationPreference>()
    singleOf(::SupabasePushDeviceRemoteDataSource).bind<PushDeviceRemoteDataSource>()
    singleOf(::PushDeviceMapperImpl).bind<PushDeviceMapper>()
    singleOf(::PushDeviceRepositoryImpl).bind<PushDeviceRepository>()
    factoryOf(::SetPushDeviceUseCase).bind<SetPushDevice>()
    single<ClientPlatform> { currentClientPlatform }
    single {
        AppStores(
            googlePlay = StoreConfig.GOOGLE_PLAY,
            appStoreId = StoreConfig.APP_STORE_ID.ifBlank { null },
        )
    }
    singleOf(::RoomAppInviteLocalDataSource).bind<AppInviteLocalDataSource>()
    singleOf(::SupabaseAppInviteRemoteDataSource).bind<AppInviteRemoteDataSource>()
    singleOf(::AppInviteRepositoryImpl).bind<AppInviteRepository>()
    factoryOf(::ObserveAppInviteUseCase).bind<ObserveAppInvite>()
    factoryOf(::DismissAppInviteUseCase).bind<DismissAppInvite>()
}
