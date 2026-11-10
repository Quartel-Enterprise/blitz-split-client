package com.quare.blitzsplit.shared.storescreenshots

import com.quare.blitzsplit.core.expenses.domain.model.ReceiptItem
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptScan
import com.quare.blitzsplit.core.expenses.domain.usecase.ReadReceipt
import com.quare.blitzsplit.core.provider.image.BillImage
import com.quare.blitzsplit.core.sync.domain.usecase.StartSync
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.flow.flowOf
import org.koin.core.module.Module
import org.koin.dsl.module
import software.quare.connectivity.NetworkConnectivityObserver

private const val UNREACHABLE_SERVER = "http://127.0.0.1:9"
private const val RECEIPT_WIDTH = 1200
private const val RECEIPT_HEIGHT = 1800

/*
 * Why: the store screenshots show the app online, with nothing waiting to sync, but never reach the real server: the
 * sync never starts, and anything that still asks the server asks a port nothing listens on, so no anonymous account
 * is made and no sample bill goes up. Reading a receipt answers with the trattoria's items, as the server would.
 */
internal fun storeScreenshotsModuleOf(trip: SampleTrip): Module = module {
    single<StartSync> { StartSync {} }
    single<NetworkConnectivityObserver> { NetworkConnectivityObserver { flowOf(true) } }
    single<SupabaseClient> {
        createSupabaseClient(
            supabaseUrl = UNREACHABLE_SERVER,
            supabaseKey = "store-screenshots",
        ) {
            install(Auth)
            install(Postgrest)
            install(Functions)
            install(Storage)
            install(Realtime)
        }
    }
    factory<ReadReceipt> {
        ReadReceipt { source, _, _ ->
            val dinner = trip.bills.first { it.items != null }
            val items = checkNotNull(dinner.items)
            ReceiptScan.Read(
                photo = BillImage(
                    jpeg = source,
                    width = RECEIPT_WIDTH,
                    height = RECEIPT_HEIGHT,
                ),
                reading = ReceiptReading(
                    totalMinor = dinner.totalMinor,
                    description = dinner.description,
                    categoryId = dinner.categoryKey,
                    date = null,
                    time = null,
                    items = items.items.map { ReceiptItem(it.name, it.quantity, it.unitPriceMinor) },
                    servicePercent = items.servicePercent,
                ),
            )
        }
    }
}
