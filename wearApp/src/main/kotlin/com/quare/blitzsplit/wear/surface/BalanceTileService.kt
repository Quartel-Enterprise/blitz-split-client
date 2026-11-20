package com.quare.blitzsplit.wear.surface

import android.content.Context
import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.shared.settings.LegacyAppLocale
import com.quare.blitzsplit.wear.MainActivity
import com.quare.blitzsplit.wear.R
import com.quare.blitzsplit.wear.ui.theme.WearColors
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject
import kotlin.time.Duration.Companion.minutes

/*
 * Why: the "Balances" Tile (15.1): the overall balance with its word, what you're owed and what you owe, and "Open";
 * all settled (15.1b): "All settled", "Nothing to pay or receive". From the watch's own Room, like the app.
 */
class BalanceTileService : TileService() {
    // Why: Profile › Language's pick before Android 13, as every screen of the app follows it.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LegacyAppLocale.wrap(newBase))
    }

    private val brandColorScheme = ColorScheme(
        primary = WearColors.PRIMARY.toInt().argb,
        onPrimary = WearColors.ON_PRIMARY.toInt().argb,
        primaryContainer = WearColors.PRIMARY_CONTAINER.toInt().argb,
        onPrimaryContainer = WearColors.ON_PRIMARY_CONTAINER.toInt().argb,
        onSurface = WearColors.ON_SURFACE.toInt().argb,
        onSurfaceVariant = WearColors.ON_SURFACE_VARIANT.toInt().argb,
    )

    // Why: the totals change with a sync or a payment, which ask for an update themselves; this only catches up.
    private val freshness = 30.minutes

    private val observePeopleBalances: ObservePeopleBalances by inject()

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        SuspendToFutureAdapter.launchFuture {
            val balance = GlanceBalance.of(this@BalanceTileService, observePeopleBalances().first())
            val layout = materialScope(
                context = this@BalanceTileService,
                deviceConfiguration = requestParams.deviceConfiguration,
                // Why: the app's own blues, as the watch app draws them, never the wallpaper's.
                allowDynamicTheme = false,
                defaultColorScheme = brandColorScheme,
            ) {
                primaryLayout(
                    titleSlot = { text(getString(R.string.tile_title).layoutString) },
                    mainSlot = { if (balance == null) buildSettledContent() else buildBalanceContent(balance) },
                    bottomSlot = {
                        textEdgeButton(
                            onClick = clickable(action = openAppAction()),
                            labelContent = { text(getString(R.string.tile_open).layoutString) },
                        )
                    },
                )
            }
            TileBuilders.Tile
                .Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setFreshnessIntervalMillis(freshness.inWholeMilliseconds)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
                .build()
        }

    @Deprecated("Tiles still ask for the resources of their version", ReplaceWith(""))
    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> = SuspendToFutureAdapter.launchFuture {
        ResourceBuilders.Resources
            .Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
    }

    private fun MaterialScope.buildBalanceContent(balance: GlanceBalance): LayoutElementBuilders.LayoutElement {
        val color = LayoutColor(if (balance.netMinor < 0) WearColors.TO_PAY.toInt() else WearColors.TO_RECEIVE.toInt())
        return LayoutElementBuilders.Column
            .Builder()
            .addContent(
                text(
                    balance.netText.layoutString,
                    typography = Typography.NUMERAL_SMALL,
                    color = color,
                ),
            ).addContent(
                text(
                    getString(if (balance.netMinor < 0) R.string.tile_to_pay_total else R.string.tile_to_receive_total)
                        .layoutString,
                    typography = Typography.LABEL_SMALL,
                    color = color,
                ),
            ).addContent(
                text(
                    getString(R.string.tile_owed_to_you, balance.toReceiveText).layoutString,
                    typography = Typography.BODY_SMALL,
                ),
            ).addContent(
                text(
                    getString(R.string.tile_you_owe, balance.toPayText).layoutString,
                    typography = Typography.BODY_SMALL,
                ),
            ).build()
    }

    private fun MaterialScope.buildSettledContent(): LayoutElementBuilders.LayoutElement = LayoutElementBuilders.Column
        .Builder()
        .addContent(text(getString(R.string.tile_settled_title).layoutString, typography = Typography.TITLE_MEDIUM))
        .addContent(text(getString(R.string.tile_settled_body).layoutString, typography = Typography.BODY_SMALL))
        .build()

    private fun openAppAction(): ActionBuilders.Action = ActionBuilders.LaunchAction
        .Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity
                .Builder()
                .setPackageName(packageName)
                .setClassName(MainActivity::class.java.name)
                .build(),
        ).build()

    private companion object {
        const val RESOURCES_VERSION = "1"
    }
}
