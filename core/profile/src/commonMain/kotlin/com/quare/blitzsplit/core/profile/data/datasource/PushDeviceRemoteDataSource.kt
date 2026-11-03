package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.RegisterPushDeviceParamsDto

interface PushDeviceRemoteDataSource {
    suspend fun register(params: RegisterPushDeviceParamsDto)

    suspend fun unregister(token: String)
}
