package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveAllMemberPaymentKeys
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePaymentKeys
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveAppLanguage
import com.quare.blitzsplit.core.sync.domain.model.PixSignals
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveShowsPix
import com.quare.blitzsplit.core.utils.DeviceLanguage
import com.quare.blitzsplit.core.utils.DeviceRegion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveShowsPixUseCase(
    private val observeAppLanguage: ObserveAppLanguage,
    private val observePaymentKeys: ObservePaymentKeys,
    private val observeAllMemberPaymentKeys: ObserveAllMemberPaymentKeys,
    private val observeGroups: ObserveGroups,
    private val deviceLanguage: DeviceLanguage,
    private val deviceRegion: DeviceRegion,
) : ObserveShowsPix {
    override fun invoke(): Flow<Boolean> = combine(
        observeAppLanguage(),
        observePaymentKeys(),
        observeAllMemberPaymentKeys(),
        observeGroups(),
    ) { language, ownKeys, memberKeys, groups ->
        PixSignals(
            appLanguage = language,
            deviceLanguage = deviceLanguage(),
            deviceRegion = deviceRegion(),
            hasOwnPixKey = ownKeys.any { it.type.method == PaymentMethod.PIX },
            groupCurrencies = groups.map { it.currency },
            hasMemberWithPixKey = memberKeys.any { it.type.method == PaymentMethod.PIX },
        ).showsPix
    }.distinctUntilChanged()
}
