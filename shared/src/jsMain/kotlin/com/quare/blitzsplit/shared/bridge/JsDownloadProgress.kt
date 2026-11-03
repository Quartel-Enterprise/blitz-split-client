@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.feature.groups.presentation.model.DownloadProgress

/*
 * Why: "Sincronizando… 3 de 7 grupos" (5.1d) while a new device's first download brings the groups down one by one;
 * groupCount and fraction are null until the server said how many are coming (a bar that runs meanwhile).
 */
class JsDownloadProgress(
    val arrivedGroupCount: Int,
    val groupCount: Int?,
    val fraction: Double?,
    // Why: the grey rows under the groups that came, holding the place of a few still coming.
    val placeholderCount: Int,
    // Why: offline, "Sem internet. Seus grupos aparecem quando reconectar." instead of the count.
    val isWaitingForInternet: Boolean,
)

internal fun DownloadProgress.toJsDownloadProgress(): JsDownloadProgress = JsDownloadProgress(
    arrivedGroupCount = arrivedGroupCount,
    groupCount = groupCount,
    fraction = fraction?.toDouble(),
    placeholderCount = placeholderCount,
    isWaitingForInternet = isWaitingForInternet,
)
