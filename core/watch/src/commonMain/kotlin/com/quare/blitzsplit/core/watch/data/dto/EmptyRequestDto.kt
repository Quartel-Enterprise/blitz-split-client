package com.quare.blitzsplit.core.watch.data.dto

import kotlinx.serialization.Serializable

// Why: the request function takes no field, but an edge function's body is JSON.
@Serializable
class EmptyRequestDto
