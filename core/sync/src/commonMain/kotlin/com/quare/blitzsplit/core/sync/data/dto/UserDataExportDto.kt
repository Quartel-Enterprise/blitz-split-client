package com.quare.blitzsplit.core.sync.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Why: the file people take elsewhere, so its field names and codes stay stable across versions; a change
 * of shape bumps the version. Groups and expenses join it once the device holds them.
 */
@Serializable
data class UserDataExportDto(
    @SerialName("format") val format: String,
    @SerialName("version") val version: Int,
    @SerialName("exportedAt") val exportedAt: String,
    @SerialName("account") val account: AccountExportDto,
    @SerialName("profile") val profile: ProfileExportDto?,
    @SerialName("paymentKeys") val paymentKeys: List<PaymentKeyExportDto>,
)
