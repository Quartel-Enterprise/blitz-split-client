package com.quare.blitzsplit.core.inappupdate.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: the iTunes Search API's lookup, of which only the version and the store page are read.
@Serializable
internal data class AppStoreLookupDto(
    @SerialName("results") val results: List<AppStoreLookupResultDto>?,
)
