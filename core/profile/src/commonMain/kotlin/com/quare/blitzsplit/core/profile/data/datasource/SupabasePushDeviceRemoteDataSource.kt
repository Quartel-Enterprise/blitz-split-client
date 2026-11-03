package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.RegisterPushDeviceParamsDto
import com.quare.blitzsplit.core.profile.data.dto.UnregisterPushDeviceParamsDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc

class SupabasePushDeviceRemoteDataSource(
    private val postgrest: Postgrest,
) : PushDeviceRemoteDataSource {
    override suspend fun register(params: RegisterPushDeviceParamsDto) {
        postgrest.rpc(
            function = "register_push_device",
            parameters = params,
        )
    }

    override suspend fun unregister(token: String) {
        postgrest.rpc(
            function = "unregister_push_device",
            parameters = UnregisterPushDeviceParamsDto(token),
        )
    }
}
