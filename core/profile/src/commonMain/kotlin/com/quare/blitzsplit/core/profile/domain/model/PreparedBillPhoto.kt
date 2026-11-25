package com.quare.blitzsplit.core.profile.domain.model

import com.quare.blitzsplit.core.model.PhotoFrame
import com.quare.blitzsplit.core.provider.image.BillImage

// Why: `frame` only when the image is the whole photo; a square image is its own tile.
class PreparedBillPhoto(
    val image: BillImage,
    val frame: PhotoFrame?,
)
